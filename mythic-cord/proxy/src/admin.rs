

use crate::state::ProxyState;
use http_body_util::Full;
use hyper::body::Bytes;
use hyper::server::conn::http1;
use hyper::service::service_fn;
use hyper::{Method, Request, Response, StatusCode};
use hyper_util::rt::TokioIo;
use mythiccord_stdb_bridge::ServerStatus;
use std::convert::Infallible;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::TcpListener;
use tracing::{info, warn};

pub async fn run(bind: SocketAddr, state: Arc<ProxyState>) -> anyhow::Result<()> {
    let listener = TcpListener::bind(bind).await?;
    info!(%bind, "admin HTTP listening");
    loop {
        let (stream, _) = listener.accept().await?;
        let io = TokioIo::new(stream);
        let state = state.clone();
        tokio::spawn(async move {
            let svc = service_fn(move |req| handle(req, state.clone()));
            if let Err(err) = http1::Builder::new().serve_connection(io, svc).await {
                warn!(?err, "admin HTTP connection error");
            }
        });
    }
}

#[allow(clippy::unused_async)]
async fn handle(
    req: Request<hyper::body::Incoming>,
    state: Arc<ProxyState>,
) -> Result<Response<Full<Bytes>>, Infallible> {
    let response = match (req.method(), req.uri().path()) {
        (&Method::GET, "/health") => health(&state),
        (&Method::GET, "/metrics") => metrics(&state),
        (&Method::POST, "/admin/drain") => drain(&state),
        _ => Response::builder()
            .status(StatusCode::NOT_FOUND)
            .body(Full::new(Bytes::from_static(b"not found")))
            .unwrap(),
    };
    Ok(response)
}

fn health(state: &ProxyState) -> Response<Full<Bytes>> {
    let status = *state.status.read();

    let code = match status {
        ServerStatus::Offline => StatusCode::SERVICE_UNAVAILABLE,
        _ => StatusCode::OK,
    };
    let body = serde_json::json!({
        "status": status.wire(),
        "shard": state.identity.shard_id,
        "role": state.identity.role.wire(),
        "registry_known_shards": state.registry.len(),
    })
    .to_string();
    Response::builder()
        .status(code)
        .header("content-type", "application/json")
        .body(Full::new(Bytes::from(body)))
        .unwrap()
}

fn metrics(state: &ProxyState) -> Response<Full<Bytes>> {
    let status = *state.status.read();
    let status_num = match status {
        ServerStatus::Starting => 0,
        ServerStatus::Healthy => 1,
        ServerStatus::Degraded => 2,
        ServerStatus::Draining => 3,
        ServerStatus::Offline => 4,
    };
    let body = format!(
        "# HELP mythiccord_status Proxy status enum (0=starting,1=healthy,2=degraded,3=draining,4=offline)\n\
         # TYPE mythiccord_status gauge\n\
         mythiccord_status{{shard=\"{shard}\",role=\"{role}\"}} {status_num}\n\
         # HELP mythiccord_registry_shards Number of shards visible to this proxy\n\
         # TYPE mythiccord_registry_shards gauge\n\
         mythiccord_registry_shards {shards}\n",
        shard = state.identity.shard_id,
        role = state.identity.role.wire(),
        status_num = status_num,
        shards = state.registry.len(),
    );
    Response::builder()
        .header("content-type", "text/plain; version=0.0.4")
        .body(Full::new(Bytes::from(body)))
        .unwrap()
}

fn drain(state: &ProxyState) -> Response<Full<Bytes>> {
    {
        let mut guard = state.status.write();
        if matches!(*guard, ServerStatus::Healthy | ServerStatus::Starting) {
            *guard = ServerStatus::Draining;
        }
    }
    info!("admin /drain invoked — status → Draining");
    Response::builder()
        .status(StatusCode::ACCEPTED)
        .body(Full::new(Bytes::from_static(b"draining\n")))
        .unwrap()
}
