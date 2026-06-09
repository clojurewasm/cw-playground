//! Mandelbrot escape-time, compiled to wasm32-unknown-unknown (no imports).
//! Demonstrates f64 arguments/returns across the ClojureWasm FFI boundary.

/// Iterations until |z| escapes (>2) for c = cx + cy*i, capped at max_iter.
/// Returns max_iter if the point is (probably) in the set.
#[no_mangle]
pub extern "C" fn escape(cx: f64, cy: f64, max_iter: i32) -> i32 {
    let mut zx = 0.0_f64;
    let mut zy = 0.0_f64;
    let mut i = 0;
    while i < max_iter && zx * zx + zy * zy <= 4.0 {
        let xt = zx * zx - zy * zy + cx;
        zy = 2.0 * zx * zy + cy;
        zx = xt;
        i += 1;
    }
    i
}

/// Smooth (continuous) escape value scaled by 1000 and returned as i32,
/// so callers get sub-iteration colour banding without needing f64 out.
#[no_mangle]
pub extern "C" fn escape_smooth_milli(cx: f64, cy: f64, max_iter: i32) -> i32 {
    let mut zx = 0.0_f64;
    let mut zy = 0.0_f64;
    let mut i = 0;
    while i < max_iter && zx * zx + zy * zy <= 256.0 {
        let xt = zx * zx - zy * zy + cx;
        zy = 2.0 * zx * zy + cy;
        zx = xt;
        i += 1;
    }
    if i >= max_iter {
        return max_iter * 1000;
    }
    // mu = i + 1 - log2(log(|z|)); approximate log via a cheap series-free path.
    let mag2 = zx * zx + zy * zy;
    // log(mag2)/2 = ln|z|; ln approximated by frexp-style reduction.
    let ln_mag = ln_approx(mag2) * 0.5;
    let nu = log2_approx(ln_mag);
    let mu = i as f64 + 1.0 - nu;
    (mu * 1000.0) as i32
}

fn ln_approx(x: f64) -> f64 {
    // ln via atanh series: ln(x) = 2*atanh((x-1)/(x+1)); reduce x into [0.5,2).
    if x <= 0.0 {
        return 0.0;
    }
    let mut e = 0i32;
    let mut m = x;
    while m >= 2.0 {
        m *= 0.5;
        e += 1;
    }
    while m < 0.5 {
        m *= 2.0;
        e -= 1;
    }
    let t = (m - 1.0) / (m + 1.0);
    let t2 = t * t;
    let series = 2.0 * t * (1.0 + t2 / 3.0 + t2 * t2 / 5.0 + t2 * t2 * t2 / 7.0);
    series + e as f64 * core::f64::consts::LN_2
}

fn log2_approx(x: f64) -> f64 {
    ln_approx(x) / core::f64::consts::LN_2
}
