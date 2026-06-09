//! Pure-compute number theory, compiled to wasm32-unknown-unknown.
//! No imports, no WASI — loads in ClojureWasm today via (wasm/load ...).
//! Every export takes/returns only i32/i64 (the scalar types cljw marshals).

#[no_mangle]
pub extern "C" fn gcd(mut a: i64, mut b: i64) -> i64 {
    a = a.abs();
    b = b.abs();
    while b != 0 {
        let t = b;
        b = a % b;
        a = t;
    }
    a
}

#[no_mangle]
pub extern "C" fn lcm(a: i64, b: i64) -> i64 {
    if a == 0 || b == 0 {
        return 0;
    }
    (a / gcd(a, b)).saturating_mul(b).abs()
}

#[no_mangle]
pub extern "C" fn is_prime(n: i64) -> i32 {
    if n < 2 {
        return 0;
    }
    if n % 2 == 0 {
        return (n == 2) as i32;
    }
    let mut i: i64 = 3;
    while i.saturating_mul(i) <= n {
        if n % i == 0 {
            return 0;
        }
        i += 2;
    }
    1
}

/// The 1-indexed n-th prime (nth_prime(1) == 2).
#[no_mangle]
pub extern "C" fn nth_prime(n: i32) -> i64 {
    if n < 1 {
        return -1;
    }
    let mut count = 0;
    let mut candidate: i64 = 1;
    while count < n {
        candidate += 1;
        if is_prime(candidate) == 1 {
            count += 1;
        }
    }
    candidate
}

/// Modular exponentiation: (base^exp) mod m, overflow-safe via i128 intermediates.
#[no_mangle]
pub extern "C" fn pow_mod(base: i64, exp: i64, m: i64) -> i64 {
    if m <= 1 {
        return 0;
    }
    let mut result: i128 = 1;
    let mut b: i128 = (base % m) as i128;
    if b < 0 {
        b += m as i128;
    }
    let mut e = exp;
    while e > 0 {
        if e & 1 == 1 {
            result = (result * b) % m as i128;
        }
        b = (b * b) % m as i128;
        e >>= 1;
    }
    result as i64
}

/// Euler's totient φ(n): count of integers in 1..=n coprime to n.
#[no_mangle]
pub extern "C" fn totient(mut n: i64) -> i64 {
    if n <= 0 {
        return 0;
    }
    let mut result = n;
    let mut p: i64 = 2;
    while p.saturating_mul(p) <= n {
        if n % p == 0 {
            while n % p == 0 {
                n /= p;
            }
            result -= result / p;
        }
        p += 1;
    }
    if n > 1 {
        result -= result / n;
    }
    result
}
