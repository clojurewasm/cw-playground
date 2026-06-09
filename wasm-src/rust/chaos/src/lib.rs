//! Chaotic dynamical systems, compiled to wasm32-unknown-unknown (no imports).
//! Demonstrates f64 in AND f64 out across the ClojureWasm FFI boundary.

/// Logistic map x_{n+1} = r * x_n * (1 - x_n), iterated `steps` times.
#[no_mangle]
pub extern "C" fn logistic(r: f64, x0: f64, steps: i32) -> f64 {
    let mut x = x0;
    let mut i = 0;
    while i < steps {
        x = r * x * (1.0 - x);
        i += 1;
    }
    x
}

/// X-coordinate of the Lorenz attractor after `steps` Euler steps of size `dt`,
/// using the classic chaotic parameters (sigma=10, rho=28, beta=8/3).
#[no_mangle]
pub extern "C" fn lorenz_x(steps: i32, dt: f64) -> f64 {
    let (sigma, rho, beta) = (10.0_f64, 28.0_f64, 8.0_f64 / 3.0);
    let (mut x, mut y, mut z) = (1.0_f64, 1.0_f64, 1.0_f64);
    let mut i = 0;
    while i < steps {
        let dx = sigma * (y - x);
        let dy = x * (rho - z) - y;
        let dz = x * y - beta * z;
        x += dx * dt;
        y += dy * dt;
        z += dz * dt;
        i += 1;
    }
    x
}

/// Henon map iteration count until the orbit diverges (|x|>1e6), capped.
/// Demonstrates mixing f64 args with an i32 cap and i32 result.
#[no_mangle]
pub extern "C" fn henon_escape(a: f64, b: f64, max_iter: i32) -> i32 {
    let (mut x, mut y) = (0.0_f64, 0.0_f64);
    let mut i = 0;
    while i < max_iter {
        let xn = 1.0 - a * x * x + y;
        y = b * x;
        x = xn;
        if x.abs() > 1.0e6 || y.abs() > 1.0e6 {
            return i;
        }
        i += 1;
    }
    max_iter
}
