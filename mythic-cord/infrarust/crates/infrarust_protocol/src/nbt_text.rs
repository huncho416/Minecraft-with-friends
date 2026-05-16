//! JSON Text Component → Minecraft Network NBT encoder.
//!
//! Minecraft 1.21.6+ switched several disconnect packets (CLoginDisconnect
//! among them) from JSON-string chat components to Network-NBT text
//! components. This module accepts a serialized JSON Component (as Mojang's
//! GsonComponentSerializer emits) and produces the corresponding Network
//! NBT byte stream the client expects.
//!
//! Network NBT root format (1.20.2+):
//! - Tag type byte `0x0A` (Compound), no root name length/bytes
//! - Compound payload (children + TAG_End)

const TAG_END: u8 = 0;
const TAG_BYTE: u8 = 1;
const TAG_STRING: u8 = 8;
const TAG_LIST: u8 = 9;
const TAG_COMPOUND: u8 = 10;

/// Serializes a JSON-format text component into Network NBT bytes.
///
/// On any JSON parse error, falls back to a single-text component containing
/// the raw input so the player at least sees something legible.
#[must_use]
pub fn json_text_to_network_nbt(json: &str) -> Vec<u8> {
    let value: serde_json::Value = serde_json::from_str(json)
        .unwrap_or_else(|_| serde_json::json!({ "text": json }));
    let mut out = Vec::with_capacity(64);
    out.push(TAG_COMPOUND);
    write_compound_payload(&mut out, &value);
    out
}

fn write_compound_payload(out: &mut Vec<u8>, value: &serde_json::Value) {
    let obj = match value {
        serde_json::Value::Object(map) => map.clone(),
        serde_json::Value::String(s) => {
            let mut map = serde_json::Map::new();
            map.insert("text".into(), serde_json::Value::String(s.clone()));
            map
        }
        serde_json::Value::Array(arr) => {
            let mut map = serde_json::Map::new();
            map.insert("text".into(), serde_json::Value::String(String::new()));
            map.insert("extra".into(), serde_json::Value::Array(arr.clone()));
            map
        }
        _ => {
            let mut map = serde_json::Map::new();
            map.insert("text".into(), serde_json::Value::String(value.to_string()));
            map
        }
    };
    let has_text = obj.contains_key("text");
    if !has_text {
        write_string_field(out, "text", "");
    }
    for (key, child) in &obj {
        match child {
            serde_json::Value::String(s) => write_string_field(out, key, s),
            serde_json::Value::Bool(b) => write_byte_field(out, key, u8::from(*b)),
            serde_json::Value::Number(n) => {
                if let Some(i) = n.as_i64() {
                    write_byte_field(out, key, (i & 0xFF) as u8);
                } else {
                    write_string_field(out, key, &n.to_string());
                }
            }
            serde_json::Value::Array(items) => {
                if key == "extra" {
                    write_extra_list(out, key, items);
                } else if items.iter().all(serde_json::Value::is_string) {
                    write_string_list(out, key, items);
                } else {
                    write_extra_list(out, key, items);
                }
            }
            serde_json::Value::Object(_) => write_object_field(out, key, child),
            serde_json::Value::Null => {}
        }
    }
    out.push(TAG_END);
}

fn write_string_field(out: &mut Vec<u8>, name: &str, value: &str) {
    out.push(TAG_STRING);
    write_name(out, name);
    write_string_payload(out, value);
}

fn write_byte_field(out: &mut Vec<u8>, name: &str, value: u8) {
    out.push(TAG_BYTE);
    write_name(out, name);
    out.push(value);
}

fn write_object_field(out: &mut Vec<u8>, name: &str, value: &serde_json::Value) {
    out.push(TAG_COMPOUND);
    write_name(out, name);
    write_compound_payload(out, value);
}

fn write_extra_list(out: &mut Vec<u8>, name: &str, items: &[serde_json::Value]) {
    out.push(TAG_LIST);
    write_name(out, name);
    out.push(TAG_COMPOUND);
    let len: i32 = items.len().try_into().unwrap_or(i32::MAX);
    out.extend_from_slice(&len.to_be_bytes());
    for item in items {
        write_compound_payload(out, item);
    }
}

fn write_string_list(out: &mut Vec<u8>, name: &str, items: &[serde_json::Value]) {
    out.push(TAG_LIST);
    write_name(out, name);
    out.push(TAG_STRING);
    let len: i32 = items.len().try_into().unwrap_or(i32::MAX);
    out.extend_from_slice(&len.to_be_bytes());
    for item in items {
        if let serde_json::Value::String(s) = item {
            write_string_payload(out, s);
        } else {
            write_string_payload(out, &item.to_string());
        }
    }
}

fn write_name(out: &mut Vec<u8>, name: &str) {
    let bytes = name.as_bytes();
    let len: u16 = bytes.len().try_into().unwrap_or(u16::MAX);
    out.extend_from_slice(&len.to_be_bytes());
    out.extend_from_slice(bytes);
}

fn write_string_payload(out: &mut Vec<u8>, value: &str) {
    let bytes = value.as_bytes();
    let len: u16 = bytes.len().try_into().unwrap_or(u16::MAX);
    out.extend_from_slice(&len.to_be_bytes());
    out.extend_from_slice(bytes);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encodes_simple_text() {
        let bytes = json_text_to_network_nbt(r#"{"text":"Hello"}"#);
        assert_eq!(bytes[0], TAG_COMPOUND);
        assert_eq!(bytes[1], TAG_STRING);
    }

    #[test]
    fn handles_extra_list() {
        let bytes = json_text_to_network_nbt(
            r#"{"text":"Hi","extra":[{"text":"there","color":"red"}]}"#,
        );
        assert!(bytes.windows(4).any(|w| w == b"text"));
        assert!(bytes.windows(5).any(|w| w == b"extra"));
        assert!(bytes.windows(5).any(|w| w == b"color"));
    }

    #[test]
    fn falls_back_on_garbage() {
        let bytes = json_text_to_network_nbt("not-json");
        assert_eq!(bytes[0], TAG_COMPOUND);
    }
}
