precision highp float;

uniform vec2 u_resolution;
uniform float u_time;
uniform vec3 u_background;
uniform vec3 u_accent;
uniform vec3 u_secondary;
uniform float u_page;

void addWave(
  vec2 q,
  vec2 direction,
  float frequency,
  float speed,
  float phase,
  float weight,
  float t,
  inout float height,
  inout vec2 gradient,
  inout float laplacian
) {
  float angle = dot(q, direction) * frequency - t * speed + phase;
  float sine = sin(angle);
  float cosine = cos(angle);
  height += sine * weight;
  gradient += cosine * frequency * direction * weight;
  laplacian -= sine * frequency * frequency * weight;
}

void waveSurface(
  vec2 p,
  float t,
  out float height,
  out vec2 gradient,
  out float laplacian
) {
  vec2 warp = vec2(
    sin(p.y * 1.18 + t * 0.18),
    cos(p.x * 1.06 - t * 0.15)
  ) * 0.075;
  vec2 q = p + warp;
  height = 0.0;
  gradient = vec2(0.0);
  laplacian = 0.0;
  addWave(q, vec2( 0.0000, -1.0000), 8.50, 1.000,  0.00, 1.00, t, height, gradient, laplacian);
  addWave(q, vec2( 0.4597, -0.8881), 10.03, 1.086,  1.10, 0.42, t, height, gradient, laplacian);
  addWave(q, vec2(-0.3902, -0.9207), 6.97, 0.906, -0.70, 0.36, t, height, gradient, laplacian);
  addWave(q, vec2( 0.2204, -0.9754), 12.07, 1.192,  2.00, 0.28, t, height, gradient, laplacian);
  addWave(q, vec2(-0.1699, -0.9855), 5.78, 0.825, -2.20, 0.22, t, height, gradient, laplacian);
  addWave(q, vec2( 0.0797,  0.9968), 8.93, 1.025,  0.40, 0.12, t, height, gradient, laplacian);
  height /= 1.48;
  gradient /= 1.48;
  laplacian /= 1.48;
}

// Residual foam only needs the dominant wave components. Reusing this small
// carrier at earlier times makes whitewater detach from a crest and remain as
// foam without a CPU particle simulation or another render pass.
float foamCarrier(vec2 p, float t) {
  vec2 q = p + vec2(
    sin(p.y * 1.18 + t * 0.18),
    cos(p.x * 1.06 - t * 0.15)
  ) * 0.075;
  float value = 0.0;
  value += sin(dot(q, vec2( 0.0000, -1.0000)) * 8.50 - t * 1.000) * 1.00;
  value += sin(dot(q, vec2( 0.4597, -0.8881)) * 10.03 - t * 1.086 + 1.10) * 0.42;
  value += sin(dot(q, vec2(-0.3902, -0.9207)) * 6.97 - t * 0.906 - 0.70) * 0.36;
  return value / 1.31;
}

float foamVariation(vec2 p, float t) {
  return 0.50
    + sin(dot(p, vec2(19.3, -13.7)) + t * 0.31) * 0.24
    + sin(dot(p, vec2(-31.1, 27.9)) - t * 0.19 + 1.4) * 0.16
    + sin(dot(p, vec2(53.7, 41.3)) + t * 0.11 - 0.8) * 0.10;
}

float crestRidge(float value, float lower, float center, float upper, float width) {
  return smoothstep(lower - width, center, value)
    * (1.0 - smoothstep(center, upper + width, value));
}

void main() {
  vec2 uv = gl_FragCoord.xy / max(u_resolution, vec2(1.0));
  vec2 p = (uv - 0.5) * 2.0;
  p.x *= u_resolution.x / max(u_resolution.y, 1.0);
  p.x += u_page * 0.06;

  float t = u_time * 0.66;
  float height;
  vec2 gradient;
  float laplacian;
  waveSurface(p, t, height, gradient, laplacian);
  vec3 normal = normalize(vec3(-gradient * 0.105, 1.0));

  vec3 lightDirection = normalize(vec3(
    -0.42 + sin(t * 0.13) * 0.07,
    -0.31,
    0.86
  ));
  float diffuse = max(dot(normal, lightDirection), 0.0);
  float specular = pow(
    max(dot(reflect(-lightDirection, normal), vec3(0.0, 0.0, 1.0)), 0.0),
    38.0
  );
  float fresnel = pow(1.0 - clamp(normal.z, 0.0, 1.0), 3.0);

  float pixelWidth = 2.0 / max(min(u_resolution.x, u_resolution.y), 1.0);
  float opticalWidth = 0.035 + pixelWidth * 2.8;
  float convergence = smoothstep(15.0, 64.0, -laplacian);
  float causticRidge = 1.0 - abs(fract(
    height * 0.70 + p.x * 0.19 - p.y * 0.12 + t * 0.052
  ) - 0.5) * 2.0;
  float caustics = smoothstep(
    0.77 - opticalWidth,
    0.94 + opticalWidth,
    causticRidge
  ) * (0.24 + convergence * 0.58);

  // The active whitewater is tied to the local composite crest and curvature,
  // not to a straight shoreline band. Two advected history samples use the
  // same carrier, so the shape gradually relaxes into lower-density foam.
  float crestHeight = crestRidge(
    height,
    0.38,
    0.60,
    0.82,
    opticalWidth
  );
  float crestCurvature = smoothstep(19.0, 68.0, -laplacian);
  float crestFace = 0.52 + smoothstep(0.72, 3.60, length(gradient)) * 0.34;
  float activeCrest = crestHeight * crestCurvature * crestFace;

  float variationNow = foamVariation(p, t);
  float attached = activeCrest * smoothstep(0.50, 0.78, variationNow);

  vec2 rearward = vec2(0.016, -0.025);
  float recentCarrier = foamCarrier(p + rearward, t - 0.42);
  float oldCarrier = foamCarrier(p + rearward * 2.15, t - 0.96);
  float transitionVariation = foamVariation(p + rearward * 1.4, t - 0.34);
  float residualVariation = foamVariation(p + rearward * 2.8, t - 0.82);
  float transitioning = crestRidge(
    recentCarrier,
    0.38,
    0.60,
    0.82,
    opticalWidth
  ) * smoothstep(0.54, 0.80, transitionVariation);
  float residual = crestRidge(
    oldCarrier,
    0.42,
    0.64,
    0.86,
    opticalWidth
  ) * smoothstep(0.58, 0.82, residualVariation);

  float cellular = abs(
    sin(p.x * 59.0 + p.y * 23.0 + t * 0.17)
    * sin(p.y * 67.0 - p.x * 19.0 - t * 0.13)
  );
  float bubbleDetail = smoothstep(
    0.68 - opticalWidth * 2.0,
    0.94 + opticalWidth * 2.0,
    cellular
  );
  float foam = clamp(
    attached * (0.26 + bubbleDetail * 0.10)
      + transitioning * (0.075 + bubbleDetail * 0.10)
      + residual * (0.028 + bubbleDetail * 0.065),
    0.0,
    0.34
  );

  vec3 low = u_background * 0.67;
  vec3 high = mix(u_background, u_accent, 0.10) * 0.97;
  vec3 color = mix(low, high, 0.48 + diffuse * 0.10);
  color += u_accent * caustics * 0.060;
  color += mix(u_accent, u_secondary, 0.35) * specular * 0.040;
  color += u_accent * fresnel * 0.020;
  color += u_accent * height * 0.008;

  vec3 foamColor = mix(u_accent, vec3(0.86, 0.91, 0.90), 0.34);
  color = mix(color, foamColor, foam * 0.27);
  color *= 0.91 + 0.09 * smoothstep(0.0, 1.0, uv.y);

  gl_FragColor = vec4(max(color, vec3(0.0)), 1.0);
}
