#version 150
#if defined(RENDERTYPE_TEXT) || defined(RENDERTYPE_TEXT_INTENSITY)

struct TextData {
    vec4 color;
    vec4 topColor;
    vec4 backColor;
    vec2 position;
    vec2 characterPosition;
    vec2 localPosition;
    vec2 uv;
    vec2 uvMin;
    vec2 uvMax;
    vec2 uvCenter;
    bool isShadow;
    bool doTextureLookup;
    bool shouldScale;
};

TextData textData;

// Pre-calculated time values (calculate once per frame, not per pixel)
float fastTime;
float medTime;
float slowTime;

void initTimeValues() {
    fastTime = GameTime * 500.0;
    medTime = GameTime * 1200.0;
    slowTime = GameTime * 300.0;
}

bool uvBoundsCheck(vec2 uv, vec2 uvMin, vec2 uvMax) {
    if(isnan(uv.x) || isnan(uv.y)) return true;
    const float error = 0.0001;
    return uv.x < textData.uvMin.x + error || uv.y < textData.uvMin.y + error || uv.x > textData.uvMax.x - error || uv.y > textData.uvMax.y - error;
}

// OPTIMIZED: 4-sample cross pattern instead of 9-sample grid
vec3 textSdf() {
    vec3 value = vec3(0.0, 0.0, 1.0);
    vec2 texelSize = 1.0 / vec2(256.0);
    
    // Check 4 cardinal directions + center
    vec2 offsets[5] = vec2[](
        vec2(0.0, 0.0),
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );
    
    for(int i = 0; i < 5; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;

        vec4 s = texture(Sampler0, uv);
        if(s.a >= 0.1) {
            vec3 v = vec3(fract(uv * 256.0), 0.0);
            
            if(offsets[i].x == 0.0) v.x = 0.0;
            if(offsets[i].y == 0.0) v.y = 0.0;
            if(offsets[i].x > 0.0) v.x = 1.0 - v.x;
            if(offsets[i].y > 0.0) v.y = 1.0 - v.y;
            
            v.z = length(v.xy);
            if(v.z < value.z) value = v;
        }
    }
    return value;
}

void override_text_color(vec4 color) {
    textData.color = color;
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void override_text_color(vec3 color) {
    textData.color.rgb = color;
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void override_shadow_color(vec4 color) {
    if(textData.isShadow) {
        textData.color = color;
        textData.topColor.rgb = color.rgb;
        textData.topColor.a *= color.a;
        textData.backColor.rgb = color.rgb;
        textData.backColor.a *= color.a;
    }
}

void override_shadow_color(vec3 color) {
    override_shadow_color(vec4(color, 1.0));
}

void remove_text_shadow() {
    if(textData.isShadow) textData.color.a = 0.0;
}

void apply_vertical_shadow() {
    if(textData.isShadow) {
        textData.uv.x += 1.0 / 256.0;
        textData.shouldScale = false;
    }
}

void apply_waving_movement(float speed, float frequency) {
    textData.uv.y += sin(textData.characterPosition.x * 0.1 * frequency - GameTime * 7500.0 * speed) / 256.0;
    textData.shouldScale = false;
}

void apply_waving_movement(float speed) {
    apply_waving_movement(speed, 1.0);
}

void apply_waving_movement() {
    apply_waving_movement(1.0, 1.0);
}

void apply_shaking_movement() {
    float noiseX = noise(textData.characterPosition.x + textData.characterPosition.y + GameTime * 32000.0) - 0.5;
    float noiseY = noise(textData.characterPosition.x - textData.characterPosition.y + GameTime * 32000.0) - 0.5;
    textData.shouldScale = false;
    textData.uv += vec2(noiseX, noiseY) / 256.0;
}

void apply_iterating_movement(float speed, float space) {
    float x = mod(textData.characterPosition.x * 0.4 - GameTime * 18000.0 * speed, (5.0 * space) * TAU);
    if(x > TAU) x = TAU;
    textData.uv.y += (-cos(x) * 0.5 + 0.5) / 256.0;
    textData.shouldScale = false;
}

void apply_iterating_movement() {
    apply_iterating_movement(1.0, 1.0);
}

void apply_flipping_movement(float speed, float space) {
    float t = mod((textData.characterPosition.x * 0.4 - GameTime  * 18000.0 * speed) / TAU, 5.0 * space);
    textData.uv.x = textData.uvCenter.x + (textData.uv.x - textData.uvCenter.x) / (cos(TAU * min(t, 1.0)));
    textData.uv.y = textData.uvCenter.y + (textData.uv.y - textData.uvCenter.y) / (1.0 + 0.1 * sin(TAU * min(t, 1.0)));
    textData.shouldScale = false;
}

void apply_flipping_movement() {
    apply_flipping_movement(1.0, 1.0);
}

void apply_skewing_movement(float speed) {
    float t = GameTime * 1600.0 * speed;
    textData.uv.x = mix(textData.uv.x, textData.uv.x + sin(TAU * t * 0.5) / 256.0, 1.0 - textData.localPosition.y);
    textData.uv.y = mix(textData.uv.y, textData.uvMax.y, -(0.3 + 0.5 * cos(TAU * t)));
    textData.shouldScale = false;
}

void apply_skewing_movement() {
    apply_skewing_movement(1.0);
}

void apply_growing_movement(float speed) {
    vec2 offset = vec2(0.0, 5.0 / 256.0);
    textData.uv = (textData.uv - textData.uvCenter - offset) * (sin(GameTime * 12800.0 * speed) * 0.15 + 0.85) + textData.uvCenter + offset;
    textData.shouldScale = false;
}

void apply_growing_movement() {
    apply_growing_movement(1.0);
}

void apply_spiral_embrace(float speed, float radius) {
    float t = GameTime * 4000.0 * speed;
    float angle = t + textData.characterPosition.x * 0.2;
    
    // Spiral distance from center varies over time
    float spiralDist = sin(t * 0.5) * radius;
    
    vec2 offset = vec2(
        cos(angle) * spiralDist,
        sin(angle) * spiralDist
    );
    
    textData.uv += offset / 256.0;
    textData.shouldScale = false;
}

void apply_dangling(float speed, float swayAmount) {
    float t = GameTime * 3500.0 * speed;
    
    // Pendulum motion
    float angle = sin(t + textData.characterPosition.x * 0.1) * swayAmount;
    
    // Characters further down sway more
    float swayMultiplier = textData.localPosition.y;
    
    textData.uv.x += sin(angle) * swayMultiplier * 1.5 / 256.0;
    
    // Slight vertical movement for realism
    textData.uv.y += (1.0 - cos(angle)) * swayMultiplier * 0.3 / 256.0;
    
    textData.shouldScale = false;
}

void apply_dangling_global(float speed, float swayAmount) {
    float t = GameTime * 3500.0 * speed;
    
    // Same angle for all characters (removed characterPosition offset)
    float angle = sin(t) * swayAmount;
    
    // Characters further down sway more
    float swayMultiplier = textData.localPosition.y;
    
    textData.uv.x += sin(angle) * swayMultiplier * 1.5 / 256.0;
    
    // Slight vertical movement for realism
    textData.uv.y += (1.0 - cos(angle)) * swayMultiplier * 0.3 / 256.0;
    
    textData.shouldScale = false;
}

// Text cracks apart slightly - works in lores too
void apply_crack_movement(float speed, float intensity) {
    float t = GameTime * 6000.0 * speed;
    float crackPhase = sin(t);
    
    if(crackPhase > 0.0) {
        // Use UV coordinates instead of character position for per-pixel variation
        float angle = noise((textData.uv.x + textData.uv.y) * 1000.0) * TAU;
        vec2 crackDir = vec2(cos(angle), sin(angle));
        
        float crackAmount = crackPhase * crackPhase * intensity;
        textData.uv += crackDir * crackAmount / 256.0;
    }
    
    textData.shouldScale = false;
}
void apply_crack_movement() {
    apply_crack_movement(1.0, 1.5);
}

// Text melts downward like liquid
void apply_melting_movement(float speed, float viscosity) {
    float t = GameTime * 5000.0 * speed;
    float melt = textData.characterPosition.x * 0.1 + t;
    float drip = sin(melt) * viscosity;
    
    // More melting at the bottom of each character
    float meltAmount = smoothstep(0.0, 1.0, textData.localPosition.y);
    textData.uv.y += (drip * meltAmount) / 256.0;
    
    // Slight horizontal wobble as it melts
    textData.uv.x += (sin(melt * 2.0) * 0.3 * meltAmount) / 256.0;
    textData.shouldScale = false;
}
void apply_melting_movement() {
    apply_melting_movement(1.0, 2.0);
}

// OPTIMIZED: 4 samples instead of 9
void apply_outline(vec3 color) {
    textData.shouldScale = false;

    if(textData.isShadow) {
        color *= 0.25;
        textData.color.rgb = color;
    }

    vec2 texelSize = 1.0 / vec2(256.0);
    
    // Check cardinal directions only
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );

    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;

        if(texture(Sampler0, uv).a >= 0.1) {
            textData.backColor = vec4(color, 1.0);
            return;
        }
    }
}

// OPTIMIZED: 4 samples instead of 9
void apply_thin_outline(vec3 color) {
    textData.shouldScale = false;

    if(textData.isShadow) {
        color *= 0.25;
        textData.color.rgb = color;
    }

    vec2 texelSize = 0.5 / vec2(256.0);
    
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );

    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;

        if(texture(Sampler0, uv).a >= 0.1) {
            textData.backColor = vec4(color, 1.0);
            return;
        }
    }
}

void apply_gradient(vec3 color1, vec3 color2) {
    textData.color.rgb = mix(color1, color2, (textData.uv.y - textData.uvMin.y) / (textData.uvMax.y - textData.uvMin.y));
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_animated_gradient_3(vec3 color1, vec3 color2, vec3 color3) {
    float t = 0.05 * (textData.position.x + textData.position.y) - fastTime * 10.0;
    float smoothT = fract(t / (2.0 * 3.14159)) * 3.0;

    vec3 resultColor;
    if (smoothT < 1.0) {
        resultColor = mix(color1, color2, smoothT);
    } else if (smoothT < 2.0) {
        resultColor = mix(color2, color3, smoothT - 1.0);
    } else {
        resultColor = mix(color3, color1, smoothT - 2.0);
    }

    textData.color.rgb = resultColor;
    if(textData.isShadow) textData.color.rgb *= 0.25;
    textData.shouldScale = true;
}

// OPTIMIZED: Pre-calculate wave value
void apply_animated_gradient_7(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6, vec3 color7) {
    float t = 0.01 * (textData.position.x + textData.position.y) - fastTime;
    float smoothT = fract(t) * 7.0;

    vec3 resultColor;
    if (smoothT < 1.0) {
        resultColor = mix(color1, color2, smoothT);
    } else if (smoothT < 2.0) {
        resultColor = mix(color2, color3, smoothT - 1.0);
    } else if (smoothT < 3.0) {
        resultColor = mix(color3, color4, smoothT - 2.0);
    } else if (smoothT < 4.0) {
        resultColor = mix(color4, color5, smoothT - 3.0);
    } else if (smoothT < 5.0) {
        resultColor = mix(color5, color6, smoothT - 4.0);
    } else if (smoothT < 6.0) {
        resultColor = mix(color6, color7, smoothT - 5.0);
    } else {
        resultColor = mix(color7, color1, smoothT - 6.0);
    }

    float wave = sin(10.0 * t + textData.position.x * 0.2) * 0.5 + 0.5;
    resultColor = mix(resultColor, vec3(1.0), wave * 0.15);

    textData.color.rgb = resultColor;
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_animated_gradient_5(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5) {
    float t = 0.01 * (textData.position.x + textData.position.y) - fastTime;
    float smoothT = fract(t) * 5.0;

    vec3 resultColor;
    if (smoothT < 1.0) {
        resultColor = mix(color1, color2, smoothT);
    } else if (smoothT < 2.0) {
        resultColor = mix(color2, color3, smoothT - 1.0);
    } else if (smoothT < 3.0) {
        resultColor = mix(color3, color4, smoothT - 2.0);
    } else if (smoothT < 4.0) {
        resultColor = mix(color4, color5, smoothT - 3.0);
    } else {
        resultColor = mix(color5, color1, smoothT - 4.0);
    }

    float wave = sin(10.0 * t + textData.position.x * 0.2) * 0.5 + 0.5;
    resultColor = mix(resultColor, vec3(1.0), wave * 0.15);

    textData.color.rgb = resultColor;
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_animated_gradient_7_smoother(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6, vec3 color7) {
    float t = 0.01 * (textData.position.x + textData.position.y) - fastTime;
    float smoothT = fract(t) * 12.0;
    
    // Pre-calculate for fewer operations
    float idx = floor(smoothT);
    float blend = fract(smoothT);
    
    // Use step functions to avoid branching
    vec3 resultColor = vec3(0.0);
    
    // Forward transitions (0-6)
    resultColor += mix(color1, color2, blend) * (1.0 - step(1.0, idx)) * step(0.0, idx);
    resultColor += mix(color2, color3, blend) * step(1.0, idx) * (1.0 - step(2.0, idx));
    resultColor += mix(color3, color4, blend) * step(2.0, idx) * (1.0 - step(3.0, idx));
    resultColor += mix(color4, color5, blend) * step(3.0, idx) * (1.0 - step(4.0, idx));
    resultColor += mix(color5, color6, blend) * step(4.0, idx) * (1.0 - step(5.0, idx));
    resultColor += mix(color6, color7, blend) * step(5.0, idx) * (1.0 - step(6.0, idx));
    
    // Backward transitions (6-12)
    resultColor += mix(color7, color6, blend) * step(6.0, idx) * (1.0 - step(7.0, idx));
    resultColor += mix(color6, color5, blend) * step(7.0, idx) * (1.0 - step(8.0, idx));
    resultColor += mix(color5, color4, blend) * step(8.0, idx) * (1.0 - step(9.0, idx));
    resultColor += mix(color4, color3, blend) * step(9.0, idx) * (1.0 - step(10.0, idx));
    resultColor += mix(color3, color2, blend) * step(10.0, idx) * (1.0 - step(11.0, idx));
    resultColor += mix(color2, color1, blend) * step(11.0, idx);
    
    // Optimized wave
    float wave = sin(10.0 * t + textData.position.x * 0.2) * 0.5 + 0.5;
    resultColor = mix(resultColor, vec3(1.0), wave * 0.15);
    
    // Branchless shadow multiplier
    textData.color.rgb = resultColor * mix(1.0, 0.25, float(textData.isShadow));
}

void apply_animated_gradient_6_smoother(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6) {
    float t = 0.01 * (textData.position.x + textData.position.y) - fastTime;
    float smoothT = fract(t) * 10.0;
    
    // Pre-calculate for fewer operations
    float idx = floor(smoothT);
    float blend = fract(smoothT);
    
    // Use step functions to avoid branching
    vec3 resultColor = vec3(0.0);
    
    // Forward transitions (0-5)
    resultColor += mix(color1, color2, blend) * (1.0 - step(1.0, idx)) * step(0.0, idx);
    resultColor += mix(color2, color3, blend) * step(1.0, idx) * (1.0 - step(2.0, idx));
    resultColor += mix(color3, color4, blend) * step(2.0, idx) * (1.0 - step(3.0, idx));
    resultColor += mix(color4, color5, blend) * step(3.0, idx) * (1.0 - step(4.0, idx));
    resultColor += mix(color5, color6, blend) * step(4.0, idx) * (1.0 - step(5.0, idx));
    
    // Backward transitions (5-10)
    resultColor += mix(color6, color5, blend) * step(5.0, idx) * (1.0 - step(6.0, idx));
    resultColor += mix(color5, color4, blend) * step(6.0, idx) * (1.0 - step(7.0, idx));
    resultColor += mix(color4, color3, blend) * step(7.0, idx) * (1.0 - step(8.0, idx));
    resultColor += mix(color3, color2, blend) * step(8.0, idx) * (1.0 - step(9.0, idx));
    resultColor += mix(color2, color1, blend) * step(9.0, idx);
    
    // Optimized wave
    float wave = sin(10.0 * t + textData.position.x * 0.2) * 0.5 + 0.5;
    resultColor = mix(resultColor, vec3(1.0), wave * 0.15);
    
    // Branchless shadow multiplier
    textData.color.rgb = resultColor * mix(1.0, 0.25, float(textData.isShadow));
}

void apply_character_cycle(vec3 startRGB, vec3 endRGB, float speed, float frequency) {
    float timeOffset = fastTime * speed;
    float positionOffset = textData.characterPosition.x * frequency;
    float wave = sin(positionOffset - timeOffset) * 0.5 + 0.5;

    textData.color.rgb = mix(startRGB, endRGB, wave);

    if (textData.isShadow) {
        textData.color.rgb *= 0.25;
    }
    textData.shouldScale = true;
}

void apply_rainbow() {
    textData.color.rgb = hsvToRgb(vec3(0.005 * (textData.position.x + textData.position.y) - slowTime, 0.7, 1.0));
    if(textData.isShadow) textData.color.rgb *= 0.25;
    textData.shouldScale = true;
}

void apply_pastel_rainbow() {
    vec3 hsvColor = vec3(0.005 * (textData.position.x + textData.position.y) - slowTime, 0.3, 0.95);
    textData.color.rgb = hsvToRgb(hsvColor);
    if(textData.isShadow) textData.color.rgb *= 0.25;
}

// OPTIMIZED: Simplified noise function
float fast_hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float fast_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float a = fast_hash(i);
    float b = fast_hash(i + vec2(1.0, 0.0));
    float c = fast_hash(i + vec2(0.0, 1.0));
    float d = fast_hash(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// OPTIMIZED: 2 octaves instead of 4
float fast_fbm(vec2 p) {
    float value = 0.0;
    value += 0.5 * fast_noise(p);
    value += 0.25 * fast_noise(p * 2.0);
    return value;
}

// OPTIMIZED: Reduced complexity fractal effects
void apply_config_fractal_colors(vec3 color1, vec3 color2, vec3 color3) {
    #ifdef FSH
        vec2 uv = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        float shade = fast_fbm(uv + medTime);
        shade = pow(shade, 0.7);

        vec3 blend1 = mix(color1, color2, shade);
        vec3 blend2 = mix(color2, color3, shade);
        textData.color.rgb = mix(blend1, blend2, shade);

        if(textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

// Red fractal helper functions
float red_fractal_rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float red_fractal_noise(vec2 p){
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u*u*(3.0-2.0*u);

    float res = mix(
        mix(red_fractal_rand(ip),red_fractal_rand(ip+vec2(1.0,0.0)),u.x),
        mix(red_fractal_rand(ip+vec2(0.0,1.0)),red_fractal_rand(ip+vec2(1.0,1.0)),u.x),u.y);
    return res*res;
}

float red_fractal_fbm(vec2 p) {
    return 0.516129 * red_fractal_noise(p + GameTime * 1200.0);
}

// Pink fractal helper functions
float pink_fractal_rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float pink_fractal_noise(vec2 p){
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u*u*(3.0-2.0*u);

    float res = mix(
        mix(pink_fractal_rand(ip),pink_fractal_rand(ip+vec2(1.0,0.0)),u.x),
        mix(pink_fractal_rand(ip+vec2(0.0,1.0)),pink_fractal_rand(ip+vec2(1.0,1.0)),u.x),u.y);
    return res*res;
}

float pink_fractal_fbm(vec2 p) {
    return 0.516129 * pink_fractal_noise(p + GameTime * 1200.0);
}

// The actual effect functions
void apply_red_fractal() {
    #ifdef FSH
        vec2 uv = vec2(gl_FragCoord.xy) / 100.0;
        float shade = red_fractal_fbm(uv);
        shade = pow(shade, 0.7);
        vec3 deepRed = vec3(0.4, 0.0, 0.0);
        vec3 brightRed = vec3(1.0, 0.0, 0.0);
        vec3 lightRed = vec3(1.0, 0.4, 0.4);
        vec3 paleRed = vec3(1.0, 0.5, 0.5);
        vec3 veryPaleRed = vec3(1.0, 0.9, 0.9);
        vec3 pureWhite = vec3(1.0, 1.0, 1.0);
        vec3 color1 = mix(deepRed, brightRed, shade);
        vec3 color2 = mix(brightRed, lightRed, shade);
        vec3 color3 = mix(lightRed, paleRed, shade);
        vec3 color4 = mix(paleRed, veryPaleRed, shade);
        vec3 color5 = mix(veryPaleRed, pureWhite, shade);
        vec3 finalColor1 = mix(mix(color1, color2, shade), mix(color3, color4, shade), shade);
        vec3 finalColor2 = mix(finalColor1, color5, shade);
        textData.color.rgb = finalColor2;
        if(textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_pink_fractal() {
    #ifdef FSH
        vec2 uv = vec2(gl_FragCoord.xy) / 100.0;
        float shade = pink_fractal_fbm(uv);
        vec3 lighter_dark_pink = vec3(0.6, -0.6, 0.6);
        textData.color.rgb = mix(lighter_dark_pink, vec3(3.25, 3.136, 1.75), shade);
        if(textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_glitch(float intensity, float speed) {
    textData.shouldScale = true;
    float time = floor(GameTime * speed * 6000.0);
    float glitchLine = floor(textData.uv.y * 256.0);
    if (random(vec2(glitchLine, time)) < intensity) {
        float offset = (random(vec2(glitchLine, time + 1.0)) * 2.0 - 1.0) * 3.0 / 256.0;
        textData.uv.x += offset;
        textData.uv = clamp(textData.uv, textData.uvMin, textData.uvMax);
    }
    textData.shouldScale = true;
}

void apply_egg_crack(float intensity, float speed) {
    textData.shouldScale = false;
    float time = floor(GameTime * speed * 6000.0);
    float crackLine = floor(textData.uv.x * 256.0); // split by column not row
    
    if (random(vec2(crackLine, time)) < intensity) {
        float offset = (random(vec2(crackLine, time + 1.0)) * 2.0 - 1.0) * 2.0 / 256.0;
        textData.uv.y += offset;
        textData.uv = clamp(textData.uv, textData.uvMin, textData.uvMax);
    }
}

void apply_hopping_movement(float speed) {
    float t = GameTime * 3000.0 * speed;
    float charOffset = textData.characterPosition.x * 0.08;
    
    // Hop = fast up, slow fall (like a bounce)
    float hop = -abs(sin(t + charOffset));
    textData.uv.y += hop * 1.2 / 256.0;
    
    // Slight squish on landing
    float squish = abs(cos(t + charOffset)) * 0.3;
    textData.uv.x += sin(charOffset) * squish * 0.4 / 256.0;
    
    textData.shouldScale = false;
}

void apply_rainbow_fractal() {
    #ifdef FSH
        vec2 uv = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        float fractalShade = fast_fbm(uv + medTime);
        
        vec2 smoothCoord = vec2(gl_FragCoord.xy);
        float baseHue = 0.003 * (smoothCoord.x + smoothCoord.y) - slowTime;
        
        float hue = mod(baseHue + fractalShade * 1.0, 1.0); // Full fractal influence restored

        textData.color.rgb = hsvToRgb(vec3(hue, 0.7, 1.0));

        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_pale_rainbow_fractal() {
    #ifdef FSH
        vec2 uv = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        float fractalShade = fast_fbm(uv + medTime);
        
        vec2 smoothCoord = vec2(gl_FragCoord.xy);
        float baseHue = 0.003 * (smoothCoord.x + smoothCoord.y) - slowTime;
        
        float hue = mod(baseHue + fractalShade * 1.0, 1.0);
        textData.color.rgb = hsvToRgb(vec3(hue, 0.38, 0.95));
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_pastel_outline() {
    textData.shouldScale = false;
    vec2 texelSize = 0.26 / vec2(256.0);
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );
    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;
        if(texture(Sampler0, uv).a >= 0.1) {
            #ifdef FSH

                vec2 fragUV = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
                float fractalShade = fast_fbm(fragUV + medTime);
                vec2 smoothCoord = vec2(gl_FragCoord.xy);
                float baseHue = 0.003 * (smoothCoord.x + smoothCoord.y) - slowTime;
                float hue = mod(baseHue + fractalShade * 1.0, 1.0);
                
                vec3 outlineColor = hsvToRgb(vec3(hue, 0.55, 0.60)); // Same hue, darker
                if(textData.isShadow) {
                    outlineColor *= 0.25;
                }
                textData.backColor = vec4(outlineColor, 1.0);
            #endif
            return;
        }
    }
}

void apply_fractal_color_outline(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6, vec3 color7, float speed) {
    textData.shouldScale = false;
    vec2 texelSize = 0.26 / vec2(256.0);
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );
    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;
        if(texture(Sampler0, uv).a >= 0.1) {
            #ifdef FSH
                vec2 fragUV = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
                float fractalShade = fast_fbm(fragUV + medTime * speed);

                vec2 smoothCoord = vec2(gl_FragCoord.xy);
                float baseHue = 0.003 * (smoothCoord.x + smoothCoord.y) - slowTime * speed;
                float hue = mod(baseHue + fractalShade * 3.0, 1.0);

                float colorIndex = hue * 7.0;
                int index1 = int(floor(colorIndex));
                int index2 = (index1 + 1) % 7;
                float t = fract(colorIndex);

                vec3 colors[7];
                colors[0] = color1;
                colors[1] = color2;
                colors[2] = color3;
                colors[3] = color4;
                colors[4] = color5;
                colors[5] = color6;
                colors[6] = color7;

                vec3 outlineColor = mix(colors[index1 % 7], colors[index2 % 7], t);
                outlineColor *= 0.65;

                if(textData.isShadow) outlineColor *= 0.25;
                textData.backColor = vec4(outlineColor, 1.0);
            #endif
            return;
        }
    }
}

void apply_fractal_color(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6, vec3 color7, float speed) {
    #ifdef FSH
        vec2 uv = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        float fractalShade = fast_fbm(uv + medTime * speed);
        
        vec2 smoothCoord = vec2(gl_FragCoord.xy);
        float baseHue = 0.003 * (smoothCoord.x + smoothCoord.y) - slowTime * speed;
        float hue = mod(baseHue + fractalShade * 3.0, 1.0); // Full fractal influence restored (was 3.0 originally)
        
        float colorIndex = hue * 7.0;
        int index1 = int(floor(colorIndex));
        int index2 = (index1 + 1) % 7;
        float t = fract(colorIndex);
        
        vec3 witchColors[7];
        witchColors[0] = color1;
        witchColors[1] = color2;
        witchColors[2] = color3;
        witchColors[3] = color4;
        witchColors[4] = color5;
        witchColors[5] = color6;
        witchColors[6] = color7;
        
        textData.color.rgb = mix(witchColors[index1 % 7], witchColors[index2 % 7], t);
        
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

// OPTIMIZED: Simplified noxs_veil with 2 octaves
void apply_noxs_veil(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed) {
    #ifdef FSH
        float t = medTime * speed;
        vec2 p = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        vec2 q = p * 5.0;
        
        q.x += sin(q.y * 2.0 + t) * 0.2;
        q.y += cos(q.x * 2.0 + t) * 0.2;
        
        float n1 = fast_fbm(q + t);
        float n2 = fast_fbm(q * 1.5 - t * 0.5);
        
        vec3 col = n1 * color1 + n2 * color2;
        col += fast_noise(q * 3.0 + t * 0.25) * color3 * 0.5;
        col += fast_noise(q * 3.0 - t * 0.25) * color4 * 0.5;
        col = pow(col, vec3(1.3));
        
        float star = fract(sin(dot(p * 20.0, vec2(12.9898, 78.233))) * 43758.5453);
        star = smoothstep(2.95, 5.0, star) * 0.2;
        textData.color.rgb = col + vec3(star);

        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_sliding_movement(float speed) {
    float t = GameTime * 800.0 * speed;
    
    // Very slow, smooth, large amplitude
    textData.uv.x += sin(t) * 1.5 / 256.0;       // horizontal slide
    textData.uv.y += sin(t * 0.7) * 1.0 / 256.0; // slight vertical
    
    textData.shouldScale = false;
}

void apply_shake_movement(float speed) {
    float t = GameTime * 2000.0 * speed;
    
    // Whole text drifts together slowly
    textData.uv.x += sin(t * 0.7) * 0.35 / 256.0;
    textData.uv.y += sin(t * 0.5) * 0.25 / 256.0;
    
    textData.shouldScale = false;
}

// Valentine's themed shader with floating hearts
void apply_heart(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed) {
    #ifdef FSH
        float t = medTime * speed;
        vec2 p = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        vec2 q = p * 5.0;
        
        // Gentle swirling motion
        q.x += sin(q.y * 1.5 + t) * 0.3;
        q.y += cos(q.x * 1.5 + t) * 0.3;
        
        // Base gradient layers with minimum brightness
        float n1 = fast_fbm(q + t * 0.3) * 0.7 + 0.3;
        float n2 = fast_fbm(q * 1.5 - t * 0.2) * 0.7 + 0.3;
        
        vec3 col = n1 * color1 + n2 * color2;
        col += (fast_noise(q * 2.0 + t * 0.25) * 0.5 + 0.5) * color3 * 0.4;
        
        // Heart shape function
        float heart_layer = 0.0;
        for (int i = 0; i < 8; i++) {
            vec2 offset = vec2(sin(t * 0.5 + float(i) * 0.8), cos(t * 0.3 + float(i) * 1.2)) * 3.0;
            vec2 hp = fract(q * 0.5 + offset + vec2(float(i) * 0.3)) - 0.5;
            
            // Heart equation: (x^2 + y^2 - 1)^3 - x^2*y^3 = 0
            hp.y -= 0.15; // Shift down
            hp *= 8.0; // Scale
            float heart = pow(hp.x * hp.x + hp.y * hp.y - 1.0, 3.0) - hp.x * hp.x * hp.y * hp.y * hp.y;
            heart = smoothstep(0.1, -0.1, heart);
            heart_layer += heart * (0.5 + 0.5 * sin(t + float(i)));
        }
        
        // Mix hearts into the colors
        col += heart_layer * color4 * 0.6;
        col = clamp(col, vec3(0.2), vec3(1.5)); // Prevent pure black
        col = pow(col, vec3(1.2));
        
        // Sparkles
        float sparkle = fract(sin(dot(p * 25.0, vec2(12.9898, 78.233))) * 43758.5453);
        sparkle = smoothstep(0.97, 1.0, sparkle) * 0.3;
        
        textData.color.rgb = col + vec3(sparkle);
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

// Valentine's rose petals - falling petals on pink background
void apply_rose_petals(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed) {
    #ifdef FSH
        float t = medTime * speed;
        vec2 p = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        vec2 q = p * 5.0;
        
        // Pink background gradient
        float n1 = fast_fbm(q + t * 0.2) * 0.7 + 0.3;
        float n2 = fast_fbm(q * 1.5 - t * 0.15) * 0.7 + 0.3;
        
        vec3 col = n1 * color1 + n2 * color1;
        
        // Rose petal layer
        float total_petal = 0.0;
        vec3 petal_color = vec3(0.0);
        
        for (int i = 0; i < 8; i++) {
            float fi = float(i);
            
            // Falling motion - vertical only
            vec2 offset = vec2(fi * 1.5, t * 0.5 + fi * 1.2);
            vec2 pp = fract(q * 0.5 + offset) - 0.5;
            
            // Gentle sway side to side as they fall
            pp.x += sin(t * 0.8 + fi * 2.0) * 0.1;
            
            // Rotation for tumbling
            float angle = t * 0.4 + fi * 2.0;
            float c = cos(angle);
            float s = sin(angle);
            pp = vec2(pp.x * c - pp.y * s, pp.x * s + pp.y * c);
            
            // Petal shape (elongated oval) - slightly smaller
            pp *= 10.0; // Increased from 8.0 to make petals smaller
            pp.y *= 1.6; // Make it petal-shaped
            float petal = smoothstep(1.5, 0.5, length(pp));
            
            // Pick a red color for this petal
            float color_var = fract(sin(fi * 12.9898) * 43758.5);
            vec3 this_petal_color;
            if (color_var < 0.33) {
                this_petal_color = color2;
            } else if (color_var < 0.66) {
                this_petal_color = color3;
            } else {
                this_petal_color = color4;
            }
            
            total_petal += petal;
            petal_color += petal * this_petal_color;
        }
        
        // Replace background with petal colors where petals exist
        if (total_petal > 0.01) {
            col = mix(col, petal_color / total_petal, min(total_petal, 0.8));
        }
        
        col = clamp(col, vec3(0.2), vec3(1.5));
        col = pow(col, vec3(1.2));
        
        // Sparkles
        float sparkle = fract(sin(dot(p * 25.0, vec2(12.9898, 78.233))) * 43758.5453);
        sparkle = smoothstep(0.97, 1.0, sparkle) * 0.3;
        
        textData.color.rgb = col + vec3(sparkle);
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_noxs_veil_rainbow(float speed) {
    #ifdef FSH
        float t = GameTime * 1200.0 * speed;
        vec2 p = floor(vec2(gl_FragCoord.xy) / 2.0) * 2.0 / 100.0;
        vec2 q = p * 5.0;

        q.x += sin(q.y * 2.0 + t) * 0.2;
        q.y += cos(q.x * 2.0 + t) * 0.2;

        float n1 = fast_fbm(q + t);
        float n2 = fast_fbm(q * 1.5 - t * 0.5);
        float n3 = fast_noise(q * 3.0 + t * 0.25);
        float n4 = fast_noise(q * 3.0 - t * 0.25);

        // Extended rainbow color palette for smoother loop
        vec3 rainbowColors[7];
        rainbowColors[0] = vec3(1.0, 0.0, 0.0);     // Red
        rainbowColors[1] = vec3(1.0, 0.5, 0.0);     // Orange
        rainbowColors[2] = vec3(1.0, 1.0, 0.0);     // Yellow
        rainbowColors[3] = vec3(0.0, 1.0, 0.0);     // Green
        rainbowColors[4] = vec3(0.0, 0.5, 1.0);     // Cyan
        rainbowColors[5] = vec3(0.5, 0.0, 1.0);     // Purple
        rainbowColors[6] = vec3(1.0, 0.0, 0.0);     // Red (loop back)

        // Slower time-based rainbow cycling
        float colorTime = GameTime * 150.0 * speed;
        float colorIndex = mod(colorTime, 6.0);
        int idx1 = int(floor(colorIndex));
        int idx2 = idx1 + 1;  // Next color in sequence (includes the looping red)
        float blend = fract(colorIndex);
        blend = smoothstep(0.0, 1.0, blend);

        // Pre-blend the two main colors
        vec3 baseColor = mix(rainbowColors[idx1], rainbowColors[idx2], blend);

        // Normalize noise values to create more visible contrast
        // Use wider range: darker darks and brighter brights
        float contrast1 = n1 * 1.5 - 0.25;  // Range: -0.25 to 1.25
        float contrast2 = n2 * 1.5 - 0.25;
        float contrast3 = n3 * 1.2 - 0.1;
        float contrast4 = n4 * 1.2 - 0.1;

        // Use the blended color for all noise mixing
        vec3 col = contrast1 * baseColor + contrast2 * baseColor;
        col += contrast3 * baseColor * 0.5 + contrast4 * baseColor * 0.5;

        col = clamp(col, 0.0, 1.5);
        col = pow(col, vec3(1.3));

        // Add stars
        float star = fract(sin(dot(p * 20.0, vec2(12.9898, 78.233))) * 43758.5453);
        star = smoothstep(2.95, 5.0, star) * 0.2;
        textData.color.rgb = col + vec3(star);
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

float camo_hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float camo_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = camo_hash(i);
    float b = camo_hash(i + vec2(1.0, 0.0));
    float c = camo_hash(i + vec2(0.0, 1.0));
    float d = camo_hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float camo_fbm(vec2 p) {
    float total = 0.0;
    float amplitude = 0.6;
    for (int i = 0; i < 4; i++) {
        total += camo_noise(p) * amplitude;
        p *= 1.8;
        amplitude *= 0.5;
    }
    return total;
}

void apply_camo(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed) {
    #ifdef FSH
        vec2 uv = vec2(gl_FragCoord.xy) / 50.0;
        float time = GameTime * 400.0 * speed;
        
        // Create chunky camo blobs
        float n1 = camo_fbm(uv * 2.0 + vec2(time * 0.08, 0.0));
        float n2 = camo_fbm(uv * 1.5 + vec2(100.0, time * 0.06));
        
        // Combine and create sharp transitions
        float pattern = n1 * 0.6 + n2 * 0.4;
        
        // Stretch the pattern to use full range (noise tends to cluster 0.3-0.7)
        pattern = clamp((pattern - 0.3) * 2.0, 0.0, 1.0);
        
        // Even color bands - 25% each
        vec3 color;
        if (pattern < 0.25) {
            color = color4;  // darkest
        } else if (pattern < 0.5) {
            color = color3;
        } else if (pattern < 0.75) {
            color = color2;
        } else {
            color = color1;  // brightest
        }
        
        textData.color.rgb = color;
        
        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_camo_outline(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed) {
    textData.shouldScale = false;
    
    // Only draw outline if current pixel is transparent
    float currentAlpha = texture(Sampler0, textData.uv).a;
    if(currentAlpha >= 0.1) {
        return; // Don't draw outline on the character itself
    }
    
    // Thinner outline
    vec2 texelSize = 0.26 / vec2(256.0);
    
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );
    
    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;
        if(texture(Sampler0, uv).a >= 0.1) {
            #ifdef FSH
                // Sample camo pattern at this position
                vec2 camoUV = vec2(gl_FragCoord.xy) / 50.0;
                float time = GameTime * 400.0 * speed;
                
                float n1 = camo_fbm(camoUV * 2.0 + vec2(time * 0.08, 0.0));
                float n2 = camo_fbm(camoUV * 1.5 + vec2(100.0, time * 0.06));
                
                float pattern = n1 * 0.6 + n2 * 0.4;
                pattern = clamp((pattern - 0.3) * 2.0, 0.0, 1.0);
                
                // Darker versions of the colors for outline
                vec3 darkColor1 = color1 * 0.675;
                vec3 darkColor2 = color2 * 0.675;
                vec3 darkColor3 = color3 * 0.675;
                vec3 darkColor4 = color4 * 0.675;
                
                vec3 outlineColor;
                if (pattern < 0.25) {
                    outlineColor = darkColor4;
                } else if (pattern < 0.5) {
                    outlineColor = darkColor3;
                } else if (pattern < 0.75) {
                    outlineColor = darkColor2;
                } else {
                    outlineColor = darkColor1;
                }
                
                if(textData.isShadow) {
                    outlineColor *= 0.25;
                }
                
                textData.backColor = vec4(outlineColor, 1.0);
            #endif
            return;
        }
    }
}

void apply_pastel_camo(float speed, float colorSpeed) {
    #ifdef FSH
        vec2 uv = vec2(gl_FragCoord.xy) / 50.0;
        float time = GameTime * 400.0 * speed;

        float n1 = camo_fbm(uv * 2.0 + vec2(time * 0.08, 0.0));
        float n2 = camo_fbm(uv * 1.5 + vec2(100.0, time * 0.06));

        float pattern = n1 * 0.6 + n2 * 0.4;
        pattern = clamp((pattern - 0.3) * 2.0, 0.0, 1.0);

        // Single hue cycles slowly through pastel spectrum
        float hue = mod(GameTime * 20.0 * colorSpeed, 1.0);
        vec3 baseHue = hsvToRgb(vec3(hue, 0.35, 0.95));

        // 4 shades of that hue: lightest to darkest
        // Mix toward white for light, toward a darker sat version for dark
        vec3 shade1 = mix(baseHue, vec3(1.0), 0.35);          // lightest
        vec3 shade2 = baseHue;                                  // base
        vec3 shade3 = hsvToRgb(vec3(hue, 0.50, 0.78));        // more saturated, darker
        vec3 shade4 = hsvToRgb(vec3(hue, 0.60, 0.60));        // darkest

        vec3 color;
        if (pattern < 0.25) {
            color = shade4;
        } else if (pattern < 0.5) {
            color = shade3;
        } else if (pattern < 0.75) {
            color = shade2;
        } else {
            color = shade1;
        }

        textData.color.rgb = color;

        if (textData.isShadow) {
            textData.color.rgb *= 0.25;
        }
    #endif
}

void apply_pastel_camo_outline(float speed, float colorSpeed) {
    textData.shouldScale = false;

    float currentAlpha = texture(Sampler0, textData.uv).a;
    if(currentAlpha >= 0.1) return;

    vec2 texelSize = 0.26 / vec2(256.0);
    vec2 offsets[4] = vec2[](
        vec2(texelSize.x, 0.0),
        vec2(-texelSize.x, 0.0),
        vec2(0.0, texelSize.y),
        vec2(0.0, -texelSize.y)
    );

    for(int i = 0; i < 4; i++) {
        vec2 uv = textData.uv + offsets[i];
        if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;
        if(texture(Sampler0, uv).a >= 0.1) {
            #ifdef FSH
                vec2 camoUV = vec2(gl_FragCoord.xy) / 50.0;
                float time = GameTime * 400.0 * speed;

                float n1 = camo_fbm(camoUV * 2.0 + vec2(time * 0.08, 0.0));
                float n2 = camo_fbm(camoUV * 1.5 + vec2(100.0, time * 0.06));

                float pattern = n1 * 0.6 + n2 * 0.4;
                pattern = clamp((pattern - 0.3) * 2.0, 0.0, 1.0);

                // Same hue cycle as the fill
                float hue = mod(GameTime * 20.0 * colorSpeed, 1.0);

                // Outline shades: same hue but pushed darker/more saturated
                vec3 shade1 = hsvToRgb(vec3(hue, 0.50, 0.70));
                vec3 shade2 = hsvToRgb(vec3(hue, 0.60, 0.55));
                vec3 shade3 = hsvToRgb(vec3(hue, 0.70, 0.42));
                vec3 shade4 = hsvToRgb(vec3(hue, 0.75, 0.32));

                vec3 outlineColor;
                if (pattern < 0.25) {
                    outlineColor = shade4;
                } else if (pattern < 0.5) {
                    outlineColor = shade3;
                } else if (pattern < 0.75) {
                    outlineColor = shade2;
                } else {
                    outlineColor = shade1;
                }

                if(textData.isShadow) outlineColor *= 0.25;
                textData.backColor = vec4(outlineColor, 1.0);
            #endif
            return;
        }
    }
}

void apply_character_cycle_three_colors(vec3 color1, vec3 color2, vec3 color3, float speed, float frequency) {
    float timeOffset = fastTime * speed;
    float positionOffset = textData.characterPosition.x * frequency;
    float cycle = mod(positionOffset - timeOffset, 3.0);

    vec3 resultColor;
    if (cycle < 1.0) {
        resultColor = mix(color1, color2, cycle);
    } else if (cycle < 2.0) {
        resultColor = mix(color2, color3, cycle - 1.0);
    } else {
        resultColor = mix(color3, color1, cycle - 2.0);
    }

    textData.color.rgb = resultColor;

    if (textData.isShadow) {
        textData.color.rgb *= 0.25;
    }
    textData.shouldScale = true;
}

void apply_character_cycle_four_colors(vec3 color1, vec3 color2, vec3 color3, vec3 color4, float speed, float frequency) {
    float timeOffset = fastTime * speed;
    float positionOffset = textData.characterPosition.x * frequency;
    float cycle = mod(positionOffset - timeOffset, 4.0);

    vec3 resultColor;
    if (cycle < 1.0) {
        resultColor = mix(color1, color2, cycle);
    } else if (cycle < 2.0) {
        resultColor = mix(color2, color3, cycle - 1.0);
    } else if (cycle < 3.0) {
        resultColor = mix(color3, color4, cycle - 2.0);
    } else {
        resultColor = mix(color4, color1, cycle - 3.0);
    }

    textData.color.rgb = resultColor;

    if (textData.isShadow) {
        textData.color.rgb *= 0.25;
    }
    textData.shouldScale = true;
}

void apply_character_cycle_seven_colors(vec3 color1, vec3 color2, vec3 color3, vec3 color4, vec3 color5, vec3 color6, vec3 color7, float speed, float frequency) {
    float timeOffset = fastTime * speed;
    float positionOffset = textData.characterPosition.x * frequency;
    float cycle = mod(positionOffset - timeOffset, 7.0);

    vec3 resultColor;
    if (cycle < 1.0) {
        resultColor = mix(color1, color2, cycle);
    } else if (cycle < 2.0) {
        resultColor = mix(color2, color3, cycle - 1.0);
    } else if (cycle < 3.0) {
        resultColor = mix(color3, color4, cycle - 2.0);
    } else if (cycle < 4.0) {
        resultColor = mix(color4, color5, cycle - 3.0);
    } else if (cycle < 5.0) {
        resultColor = mix(color5, color6, cycle - 4.0);
    } else if (cycle < 6.0) {
        resultColor = mix(color6, color7, cycle - 5.0);
    } else {
        resultColor = mix(color7, color1, cycle - 6.0);
    }

    textData.color.rgb = resultColor;

    if (textData.isShadow) {
        textData.color.rgb *= 0.25;
    }
    textData.shouldScale = true;
}

void apply_shimmer(float speed, float intensity) {
    if(textData.isShadow) return;
    float f = textData.localPosition.x + textData.localPosition.y - GameTime * 6400.0 * speed;

    if(mod(f, 5) < 0.75) textData.topColor = vec4(1.0, 1.0, 1.0, intensity);
    textData.shouldScale = true;
}

void apply_color_shimmer(vec3 shimmerColor, float speed, float intensity) {
    if(textData.isShadow) return;
    float f = textData.localPosition.x + textData.localPosition.y - GameTime * 6400.0 * speed;

    if(mod(f, 5) < 0.75) textData.topColor = vec4(shimmerColor, intensity);
    textData.shouldScale = true;
}

void apply_shimmer(){
    apply_shimmer(1.0, 0.5);
    textData.shouldScale = true;
}

float snow_hash(vec2 p) {
    return fract(sin(dot(p, vec2(41.1, 289.7))) * 45758.5453);
}

void apply_snow(float speed, float density, float brightness) {
    #ifdef FSH
        vec2 uv = vec2(gl_FragCoord.xy);
        float time = GameTime * 1000.0 * speed;
        
        float snow = 0.0;
        
        // Multiple layers of snow for depth
        for (int layer = 0; layer < 3; layer++) {
            float layerScale = 8.0 + float(layer) * 4.0;
            float layerSpeed = 1.0 + float(layer) * 0.3;
            
            vec2 snowUV = uv / layerScale;
            snowUV.y += time * layerSpeed * 0.05;
            snowUV.x += sin(time * 0.02 + float(layer)) * 0.5; // wind sway
            
            vec2 cell = floor(snowUV);
            vec2 local = fract(snowUV);
            
            // Random position within cell
            float randX = snow_hash(cell);
            float randY = snow_hash(cell + vec2(100.0, 0.0));
            
            vec2 snowPos = vec2(randX, randY);
            float dist = length(local - snowPos);
            
            // Snowflake size varies per layer
            float size = 0.15 - float(layer) * 0.03;
            float flake = smoothstep(size, 0.0, dist);
            
            // Fade based on layer (distant = fainter)
            flake *= (1.0 - float(layer) * 0.25);
            
            snow += flake * density;
        }
        
        // Add snow on top of existing color
        vec3 snowColor = vec3(brightness);
        textData.color.rgb = mix(textData.color.rgb, snowColor, snow * 0.8);
        textData.topColor = vec4(snowColor, snow * 0.5);
    #endif
}

void apply_snow() {
    apply_snow(1.0, 1.0, 1.0);
}

// OPTIMIZED: Cache noise calculations
void apply_chromatic_abberation(float speed, float intensity, vec4 color1, vec4 color2) {
    textData.shouldScale = true;
    
    float timeVal = GameTime * 12000.0 * speed;
    float noiseX = noise(timeVal) - 0.5;
    float noiseY = noise(timeVal + 19732.134) - 0.5;
    
    vec2 offset = vec2(0.5 / 256, 0.0) + vec2(0.5, 1.0) * vec2(noiseX, noiseY) / 256 * intensity;
    
    vec2 uv = textData.uv + offset;
    vec4 s1 = texture(Sampler0, uv);
    s1.rgb *= s1.a;
    if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) s1 = vec4(0.0);
    
    uv = textData.uv - offset;
    vec4 s2 = texture(Sampler0, uv);
    s2.rgb *= s2.a;
    if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) s2 = vec4(0.0);
    
    textData.backColor = (s1 * color1 * intensity) + (s2 * color2 * intensity);
    textData.backColor.rgb *= textData.color.rgb;
}

void apply_rainbow_abberation(float speed, float intensity) {
    textData.shouldScale = true;

    float timeVal = GameTime * 12000.0 * speed;
    float noiseX = noise(timeVal) - 0.5;
    float noiseY = noise(timeVal + 19732.134) - 0.5;

    vec2 offset = vec2(0.5 / 256, 0.0) + vec2(0.5, 1.0) * vec2(noiseX, noiseY) / 256 * intensity;

    vec4 rainbowColors[7];
    rainbowColors[0] = vec4(1.0, 0.0, 0.0, 1.0);
    rainbowColors[1] = vec4(1.0, 0.5, 0.0, 1.0);
    rainbowColors[2] = vec4(1.0, 1.0, 0.0, 1.0);
    rainbowColors[3] = vec4(0.0, 1.0, 0.0, 1.0);
    rainbowColors[4] = vec4(0.0, 0.0, 1.0, 1.0);
    rainbowColors[5] = vec4(0.3, 0.0, 0.5, 1.0);
    rainbowColors[6] = vec4(0.5, 0.0, 1.0, 1.0);

    int colorIndex1 = int(mod(GameTime * 5.0, 7.0));
    int colorIndex2 = int(mod(colorIndex1 + 3, 7.0));

    vec4 color1 = rainbowColors[colorIndex1];
    vec4 color2 = rainbowColors[colorIndex2];

    vec2 uv = textData.uv + offset;
    vec4 s1 = texture(Sampler0, uv);
    s1.rgb *= s1.a;
    if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) s1 = vec4(0.0);

    uv = textData.uv - offset;
    vec4 s2 = texture(Sampler0, uv);
    s2.rgb *= s2.a;
    if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) s2 = vec4(0.0);

    textData.backColor = (s1 * color1 * intensity) + (s2 * color2 * intensity);
    textData.backColor.rgb *= textData.color.rgb;
}

void apply_metalic(vec3 lightColor, vec3 darkColor) {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y > 3) textData.color.rgb = darkColor;
    if(y == 3) textData.color.rgb = lightColor + 0.25;
    if(y < 3) textData.color.rgb = lightColor;

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_metalic(vec3 color) {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y > 3) textData.color.rgb = color * 0.7;
    if(y == 3) textData.color.rgb = color + 0.25;
    if(y < 3) textData.color.rgb = color;

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_fire() {
    textData.shouldScale = false;
    if(textData.isShadow) return;

    float h = fract(textData.uv.y * 256.0);
    vec2 uv = textData.uv + vec2(0.0, 1.0 / 256);
    if(uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) return;
    vec4 s = texture(Sampler0, uv);
    if(s.a > 0.1) {
        float f = noise(textData.localPosition * 32.0 + vec2(0.0, GameTime * 6400.0)) * 0.5 + 0.5;
        f -= (1.0 - sqrt(h)) * 0.8;

        if(f > 0.5)
        textData.backColor = vec4(mix(vec3(1.0, 0.2, 0.2), vec3(1.0, 0.7, 0.3), (f - 0.5) / 0.5), 1.0);
    }
}


void apply_fade(float speed) {
    textData.color.a = mix(textData.color.a, 0.0, sin(GameTime * 1200 * speed * PI) * 0.5 + 0.5);
}

void apply_fade() {
    apply_fade(1.0);
}

void apply_fade(vec3 color, float speed) {
    if(textData.isShadow) color *= 0.25;

    textData.color.rgb = mix(textData.color.rgb, color, sin(GameTime * 1200 * speed * PI) * 0.5 + 0.5);
}

void apply_fade(vec3 color) {
    apply_fade(color, 1.0);
}

void apply_blinking(float speed){
    if(sin(GameTime * 3200 * speed * PI) < 0.0) { textData.color.a = 0.0; textData.backColor.a = 0.0; textData.topColor.a = 0.0; }
}

void apply_blinking() {
    apply_blinking(0.5);
}

void apply_glowing() {
    if(textData.isShadow) textData.color = vec4(0.0);
    vec3 d = textSdf();
    textData.backColor = vec4(1.0, 1.0, 1.0, (1.0 - d.z) * (1.0 - d.z));
}

void apply_lesbian_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(0.839, 0.161, 0.000);
    if(y == 3) textData.color.rgb = vec3(1.000, 0.608, 0.333);
    if(y == 4) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y == 5) textData.color.rgb = vec3(0.831, 0.384, 0.647);
    if(y >  5) textData.color.rgb = vec3(0.647, 0.000, 0.384);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_mlm_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(0.000, 0.584, 0.525);
    if(y == 3) textData.color.rgb = vec3(0.412, 0.941, 0.686);
    if(y == 4) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y == 5) textData.color.rgb = vec3(0.510, 0.698, 1.000);
    if(y >  5) textData.color.rgb = vec3(0.271, 0.153, 0.627);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_bisexual_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  4) textData.color.rgb = vec3(0.843, 0.000, 0.443);
    if(y == 4) textData.color.rgb = vec3(0.612, 0.306, 0.592);
    if(y >  4) textData.color.rgb = vec3(0.000, 0.208, 0.663);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_transgender_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(0.357, 0.812, 0.980);
    if(y == 3) textData.color.rgb = vec3(0.961, 0.671, 0.725);
    if(y == 4) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y == 5) textData.color.rgb = vec3(0.961, 0.671, 0.725);
    if(y >  5) textData.color.rgb = vec3(0.357, 0.812, 0.980);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  2) textData.color.rgb = vec3(1.000, 0.012, 0.012);
    if(y == 2) textData.color.rgb = vec3(1.000, 0.549, 0.000);
    if(y == 3) textData.color.rgb = vec3(1.000, 0.929, 0.000);
    if(y == 4) textData.color.rgb = vec3(0.000, 0.502, 0.149);
    if(y == 5) textData.color.rgb = vec3(0.000, 0.302, 1.000);
    if(y >  5) textData.color.rgb = vec3(0.459, 0.027, 0.529);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_pansexual_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(1.000, 0.129, 0.549);
    if(y == 3) textData.color.rgb = vec3(1.000, 0.847, 0.000);
    if(y == 4) textData.color.rgb = vec3(1.000, 0.847, 0.000);
    if(y >  4) textData.color.rgb = vec3(0.129, 0.694, 1.000);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_asexual_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(0.100, 0.100, 0.100);
    if(y == 3) textData.color.rgb = vec3(0.639, 0.639, 0.639);
    if(y == 4) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y >  4) textData.color.rgb = vec3(0.502, 0.000, 0.502);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_aromantic_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(0.047, 0.655, 0.294);
    if(y == 3) textData.color.rgb = vec3(0.584, 0.796, 0.451);
    if(y == 4) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y == 5) textData.color.rgb = vec3(0.639, 0.639, 0.639);
    if(y >  5) textData.color.rgb = vec3(0.100, 0.100, 0.100);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

void apply_non_binary_pride() {
    int y = int(floor((textData.uv.y - textData.uvMin.y) * 256.0));

    if(y <  3) textData.color.rgb = vec3(1.000, 0.957, 0.173);
    if(y == 3) textData.color.rgb = vec3(1.000, 1.000, 1.000);
    if(y == 4) textData.color.rgb = vec3(0.616, 0.349, 0.824);
    if(y >  4) textData.color.rgb = vec3(0.100, 0.100, 0.100);

    if(textData.isShadow) textData.color.rgb *= 0.25;
}

#define TEXT_EFFECT(r, g, b) return true; case ((uint(r/4) << 16) | (uint(g/4) << 8) | (uint(b/4))):

bool applyTextEffects() {
    uint vertexColorId = colorId(floor(round(textData.color.rgb * 255.0) / 4.0) / 255.0);
    if(textData.isShadow) { vertexColorId = colorId(textData.color.rgb);}
    switch(vertexColorId >> 8) {
        case 0xFFFFFFFFu:

    #moj_import<text_effects_config.glsl>

        return true;
    }
    return false;
}

#define SPHEYA_PACK_9

#ifdef FSH
in vec4 vctfx_screenPos;
flat in float vctfx_applyTextEffect;
flat in float vctfx_isShadow;
flat in float vctfx_changedScale;

in vec3 vctfx_ipos1;
in vec3 vctfx_ipos2;
in vec3 vctfx_ipos3;
in vec3 vctfx_ipos4;

in vec3 vctfx_uvpos1;
in vec3 vctfx_uvpos2;
in vec3 vctfx_uvpos3;
in vec3 vctfx_uvpos4;

bool applySpheyaPack9() {
    if(vctfx_applyTextEffect < 0.5) return false;

    // OPTIMIZATION: Initialize time values once
    initTimeValues();

    textData.isShadow = vctfx_isShadow > 0.5;
    textData.backColor = vec4(0.0);
    textData.topColor = vec4(0.0);
    textData.doTextureLookup = true;
    textData.color = baseColor;

    vec2 ip1 = vctfx_ipos1.xy / vctfx_ipos1.z;
    vec2 ip2 = vctfx_ipos2.xy / vctfx_ipos2.z;
    vec2 ip3 = vctfx_ipos3.xy / vctfx_ipos3.z;
    vec2 ip4 = vctfx_ipos4.xy / vctfx_ipos4.z;
    vec2 innerMin = min(ip1.xy,min(ip2.xy,min(ip3.xy,ip4.xy)));
    vec2 innerMax = max(ip1.xy,max(ip2.xy,max(ip3.xy,ip4.xy)));
    vec2 innerSize = innerMax - innerMin;

    vec2 uvp1 = vctfx_uvpos1.xy / vctfx_uvpos1.z;
    vec2 uvp2 = vctfx_uvpos2.xy / vctfx_uvpos2.z;
    vec2 uvp3 = vctfx_uvpos3.xy / vctfx_uvpos3.z;
    vec2 uvp4 = vctfx_uvpos4.xy / vctfx_uvpos4.z;
    vec2 uvMin = min(uvp1.xy,min(uvp2.xy,min(uvp3.xy, uvp4.xy)));
    vec2 uvMax = max(uvp1.xy,max(uvp2.xy,max(uvp3.xy, uvp4.xy)));
    vec2 uvSize = uvMax - uvMin;
    textData.uvMin = uvMin;
    textData.uvMax = uvMax;
    textData.uvCenter = uvMin + 0.25 * uvSize;
    textData.localPosition = ((vctfx_screenPos.xy - innerMin) / innerSize);
    textData.localPosition.y = 1.0 - textData.localPosition.y;
    textData.uv = textData.localPosition * uvSize + uvMin;
    if(vctfx_changedScale < 0.5) {
        textData.uv = texCoord0;
    }
    textData.position = vctfx_screenPos.xy * uvSize * 256.0 / innerSize;
    textData.characterPosition = 0.5 * (innerMin + innerMax) * uvSize * 256.0 / innerSize;
    if(textData.isShadow) {
        textData.characterPosition += vec2(-1.0, 1.0);
        textData.position += vec2(-1.0, 1.0);
    }
    applyTextEffects();
    if(uvBoundsCheck(textData.uv, uvMin, uvMax)) textData.doTextureLookup = false;

    vec4 textureSample = texture(Sampler0, textData.uv);

#ifdef RENDERTYPE_TEXT_INTENSITY
    textureSample = textureSample.rrrr;
    textureSample = vec4(0.0);
#endif

    if(!textData.doTextureLookup) textureSample = vec4(0.0);
    textData.topColor.a *= textureSample.a;

    fragColor = mix(vec4(textData.backColor.rgb, textData.backColor.a * textData.color.a), textureSample * textData.color, textureSample.a);
    fragColor.rgb = mix(fragColor.rgb, textData.topColor.rgb, textData.topColor.a);
    fragColor *= lightColor * ColorModulator;

    if (fragColor.a < 0.1) {
        discard;
    }

#ifdef IS_1_21_6
    fragColor = apply_fog(
        fragColor,
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd,
        FogColor
    );
#else
    fragColor = linear_fog(fragColor, vertexDistance, FogStart, FogEnd, FogColor);
#endif
    return true;
}

#endif

#ifdef VSH
out vec4 vctfx_screenPos;
flat out float vctfx_applyTextEffect;
flat out float vctfx_isShadow;
flat out float vctfx_changedScale;

out vec3 vctfx_ipos1;
out vec3 vctfx_ipos2;
out vec3 vctfx_ipos3;
out vec3 vctfx_ipos4;

out vec3 vctfx_uvpos1;
out vec3 vctfx_uvpos2;
out vec3 vctfx_uvpos3;
out vec3 vctfx_uvpos4;

bool applySpheyaPack9() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vctfx_isShadow = fract(Position.z) < 0.01 ? 1.0 : 0.0;
    vctfx_applyTextEffect = 1.0;
    vctfx_changedScale = 0.0;

    textData.isShadow = vctfx_isShadow > 0.5;
    textData.color = Color;
    textData.shouldScale = false;

    if(!applyTextEffects()) {
        vctfx_isShadow = 0.0;

#ifdef IS_1_21_6
        if (textData.isShadow)
        {
#else
        if(Position.z == 0.0 && textData.isShadow) {
#endif
            textData.isShadow = false;
            if(applyTextEffects()) {
                vctfx_isShadow = 0.0;
            }else {
                vctfx_applyTextEffect = 0.0;
                return false;
            }
        }else{
            vctfx_applyTextEffect = 0.0;
            return false;
        }
    }

    vec2 corner = vec2[](vec2(-1.0, +1.0), vec2(-1.0, -1.0), vec2(+1.0, -1.0), vec2(+1.0, +1.0))[gl_VertexID % 4];
    if(textureSize(Sampler0, 0) != ivec2(256, 256)) {
        vctfx_applyTextEffect = 0.0;
        return false;
    }

    vctfx_uvpos1 = vctfx_uvpos2 = vctfx_uvpos3 = vctfx_uvpos4 = vctfx_ipos1 = vctfx_ipos2 = vctfx_ipos3 = vctfx_ipos4 = vec3(0.0);
    switch (gl_VertexID % 4) {
        case 0: vctfx_ipos1 = vec3(gl_Position.xy, 1.0); vctfx_uvpos1 = vec3(UV0.xy, 1.0); break;
        case 1: vctfx_ipos2 = vec3(gl_Position.xy, 1.0); vctfx_uvpos2 = vec3(UV0.xy, 1.0); break;
        case 2: vctfx_ipos3 = vec3(gl_Position.xy, 1.0); vctfx_uvpos3 = vec3(UV0.xy, 1.0); break;
        case 3: vctfx_ipos4 = vec3(gl_Position.xy, 1.0); vctfx_uvpos4 = vec3(UV0.xy, 1.0); break;
    }

if(textData.shouldScale) {
    gl_Position.xy += corner * 0.03;
    vctfx_changedScale = 1.0;
}

    vctfx_screenPos = gl_Position;
#ifdef IS_1_21_6
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
#else
    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
#endif
    vertexColor = baseColor * lightColor;
    texCoord0 = UV0;
    return true;
}

#endif

#endif