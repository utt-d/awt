precision mediump float;

uniform vec2 u_resolution;
uniform float u_time;
uniform vec3 u_background;
uniform vec3 u_accent;
uniform vec3 u_secondary;
uniform float u_page;

const float TAU = 6.28318530718;

float softField(vec2 uv, vec2 center, float radius, float aspect) {
  vec2 delta = uv - center;
  delta.x *= aspect;
  float normalizedDistance = length(delta) / radius;
  float falloff = 1.0 - smoothstep(0.0, 1.0, normalizedDistance);
  return falloff * falloff;
}

void main() {
  vec2 uv = gl_FragCoord.xy / max(u_resolution, vec2(1.0));
  float aspect = u_resolution.x / max(u_resolution.y, 1.0);
  float phase = mod(u_time, 18.0) / 18.0 * TAU;
  float parallax = u_page * 0.032;

  vec2 centerOne = vec2(
    0.25 + 0.22 * cos(phase) - parallax,
    0.80 - 0.14 * sin(phase)
  );
  vec2 centerTwo = vec2(
    0.76 + 0.21 * sin(phase + 1.1) - parallax * 0.72,
    0.30 - 0.15 * cos(phase + 0.4)
  );
  vec2 centerThree = vec2(
    0.52 + 0.28 * cos(phase * 2.0 + 2.2) - parallax * 0.44,
    0.57 - 0.19 * sin(phase + 2.5)
  );
  vec2 centerFour = vec2(
    0.18 + 0.34 * sin(phase * 2.0 + 0.8) - parallax * 0.28,
    0.16 - 0.10 * cos(phase * 2.0 + 1.6)
  );

  float first = softField(uv, centerOne, 0.48, aspect);
  float second = softField(uv, centerTwo, 0.64, aspect);
  float third = softField(uv, centerThree, 0.55, aspect);
  float fourth = softField(uv, centerFour, 0.78, aspect);

  vec3 base = mix(u_background * 0.72, u_background, uv.y);
  vec3 color = base;
  color += u_accent * first * 0.20;
  color += u_secondary * second * 0.12;
  color += u_accent * third * 0.075;
  color += u_secondary * fourth * 0.055;
  color *= 0.92 + 0.08 * smoothstep(0.0, 1.0, uv.y);

  gl_FragColor = vec4(max(color, vec3(0.0)), 1.0);
}
