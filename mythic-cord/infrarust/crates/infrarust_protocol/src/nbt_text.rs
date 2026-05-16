//! JSON Text Component → Minecraft Network NBT encoder.
//!
//! Minecraft 1.20.5+ switched several disconnect packets (CLoginDisconnect
//! among them) from JSON-string chat components to Network-NBT text
//! components. This module accepts a serialized JSON Component (as Mojang's
//! GsonComponentSerializer emits) and produces the corresponding Network
//! NBT byte stream the client expects.
//!
//! Implementation: parse JSON via serde_json::Value, convert to a serde
//! struct that fastnbt knows how to serialize, then use the existing
//! to_network_nbt helper (which strips the named-root prefix).

use serde::Serialize;
use serde_json::Value;
use std::collections::BTreeMap;

/// Serializes a JSON-format text component into Network NBT bytes.
///
/// On JSON parse failure, falls back to a single-text component containing
/// the raw input so the player at least sees something legible.
#[must_use]
pub fn json_text_to_network_nbt(json: &str) -> Vec<u8> {
    let value: Value = serde_json::from_str(json).unwrap_or_else(|_| {
        let mut m = serde_json::Map::new();
        m.insert("text".into(), Value::String(json.to_string()));
        Value::Object(m)
    });
    let nbt = value_to_nbt(&value);
    crate::nbt_util::to_network_nbt(&nbt).unwrap_or_else(|_| {
        // Last-resort fallback: write a minimal compound with TAG_END only.
        vec![0x0A, 0x00]
    })
}

#[derive(Debug, Serialize)]
struct NbtComponent {
    text: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    color: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    bold: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    italic: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    underlined: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    strikethrough: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    obfuscated: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    insertion: Option<String>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    extra: Vec<NbtComponent>,
    #[serde(rename = "click_event", skip_serializing_if = "Option::is_none")]
    click_event: Option<NbtClickEvent>,
    #[serde(rename = "hover_event", skip_serializing_if = "Option::is_none")]
    hover_event: Option<NbtHoverEvent>,
}

#[derive(Debug, Serialize)]
struct NbtClickEvent {
    action: String,
    #[serde(skip_serializing_if = "String::is_empty")]
    value: String,
    #[serde(skip_serializing_if = "String::is_empty")]
    url: String,
    #[serde(skip_serializing_if = "String::is_empty")]
    command: String,
}

#[derive(Debug, Serialize)]
struct NbtHoverEvent {
    action: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    contents: Option<Box<NbtComponent>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    value: Option<Box<NbtComponent>>,
}

fn value_to_nbt(value: &Value) -> NbtComponent {
    match value {
        Value::String(s) => NbtComponent {
            text: s.clone(),
            color: None,
            bold: None,
            italic: None,
            underlined: None,
            strikethrough: None,
            obfuscated: None,
            insertion: None,
            extra: vec![],
            click_event: None,
            hover_event: None,
        },
        Value::Array(items) => NbtComponent {
            text: String::new(),
            color: None,
            bold: None,
            italic: None,
            underlined: None,
            strikethrough: None,
            obfuscated: None,
            insertion: None,
            extra: items.iter().map(value_to_nbt).collect(),
            click_event: None,
            hover_event: None,
        },
        Value::Object(map) => object_to_nbt(map),
        _ => NbtComponent {
            text: value.to_string(),
            color: None,
            bold: None,
            italic: None,
            underlined: None,
            strikethrough: None,
            obfuscated: None,
            insertion: None,
            extra: vec![],
            click_event: None,
            hover_event: None,
        },
    }
}

fn object_to_nbt(map: &serde_json::Map<String, Value>) -> NbtComponent {
    let mut out = NbtComponent {
        text: String::new(),
        color: None,
        bold: None,
        italic: None,
        underlined: None,
        strikethrough: None,
        obfuscated: None,
        insertion: None,
        extra: vec![],
        click_event: None,
        hover_event: None,
    };
    if let Some(Value::String(s)) = map.get("text") {
        out.text = s.clone();
    }
    if let Some(Value::String(s)) = map.get("color") {
        out.color = Some(s.clone());
    }
    if let Some(Value::Bool(b)) = map.get("bold") {
        out.bold = Some(*b);
    }
    if let Some(Value::Bool(b)) = map.get("italic") {
        out.italic = Some(*b);
    }
    if let Some(Value::Bool(b)) = map.get("underlined") {
        out.underlined = Some(*b);
    }
    if let Some(Value::Bool(b)) = map.get("strikethrough") {
        out.strikethrough = Some(*b);
    }
    if let Some(Value::Bool(b)) = map.get("obfuscated") {
        out.obfuscated = Some(*b);
    }
    if let Some(Value::String(s)) = map.get("insertion") {
        out.insertion = Some(s.clone());
    }
    if let Some(Value::Array(items)) = map.get("extra") {
        out.extra = items.iter().map(value_to_nbt).collect();
    }
    if let Some(click) = map.get("click_event").or_else(|| map.get("clickEvent")) {
        out.click_event = parse_click(click);
    }
    if let Some(hover) = map.get("hover_event").or_else(|| map.get("hoverEvent")) {
        out.hover_event = parse_hover(hover);
    }
    out
}

fn parse_click(value: &Value) -> Option<NbtClickEvent> {
    let obj = value.as_object()?;
    let action = obj.get("action").and_then(Value::as_str).unwrap_or("").to_string();
    let url = obj.get("url").and_then(Value::as_str).unwrap_or("").to_string();
    let command = obj.get("command").and_then(Value::as_str).unwrap_or("").to_string();
    let value_str = obj.get("value").and_then(Value::as_str).unwrap_or("").to_string();
    Some(NbtClickEvent {
        action,
        value: value_str,
        url,
        command,
    })
}

fn parse_hover(value: &Value) -> Option<NbtHoverEvent> {
    let obj = value.as_object()?;
    let action = obj.get("action").and_then(Value::as_str).unwrap_or("show_text").to_string();
    let value_field = obj.get("value").or_else(|| obj.get("contents"));
    let nested = value_field.map(|v| Box::new(value_to_nbt(v)));
    Some(NbtHoverEvent {
        action,
        contents: None,
        value: nested,
    })
}

// Keep unused import warnings quiet.
#[allow(dead_code)]
fn _btree_keep(_: BTreeMap<String, String>) {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encodes_simple_text() {
        let bytes = json_text_to_network_nbt(r#"{"text":"Hello"}"#);
        assert_eq!(bytes[0], 0x0A); // TAG_COMPOUND
        assert!(bytes.windows(4).any(|w| w == b"text"));
    }

    #[test]
    fn handles_extra_list() {
        let bytes = json_text_to_network_nbt(
            r#"{"text":"Hi","extra":[{"text":"there","color":"red"}]}"#,
        );
        assert!(bytes.windows(5).any(|w| w == b"extra"));
        assert!(bytes.windows(5).any(|w| w == b"color"));
    }

    #[test]
    fn falls_back_on_garbage() {
        let bytes = json_text_to_network_nbt("not-json");
        assert_eq!(bytes[0], 0x0A);
    }
}
