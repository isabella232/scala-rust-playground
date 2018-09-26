#![allow(non_snake_case)]

extern crate serde;
extern crate serde_json;

#[macro_use]
extern crate serde_derive;

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::str;

pub mod driver;

#[no_mangle]
pub extern "C" fn newConnection<'a>(url: *const c_char) -> *mut driver::PsqlConnection<'a> {
    return &mut driver::connect(to_string(url));
}

#[no_mangle]
pub extern "C" fn sqlQuery(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) -> *const c_char {
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let params = purepg::toGcValues(&paramsString).expect(&*format!(
        "could not convert gc values successfully: {}",
        &paramsString
    ));

    let result = conn.query(queryString, params.iter().collect());
    return to_ptr(result.to_string());
}

#[no_mangle]
pub extern "C" fn sqlExecute(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) {
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let params = purepg::toGcValues(&paramsString).expect(&*format!(
        "could not convert gc values successfully: {}",
        &paramsString
    ));

    conn.execute(queryString, params.iter().collect());
}

#[no_mangle]
pub unsafe extern "C" fn closeConnection(conn: *mut driver::PsqlConnection) {
    (*conn).close();
}

#[no_mangle]
pub extern "C" fn startTransaction<'a>(conn: *mut driver::PsqlConnection) {
    unsafe {
        (*conn).startTransaction();
    }
}

#[no_mangle]
pub extern "C" fn commitTransaction(conn: Box<driver::PsqlConnection>) {
    conn.commitTransaction();
}

#[no_mangle]
pub extern "C" fn rollbackTransaction(conn: Box<driver::PsqlConnection>) {
    conn.rollbackTransaction();
}

//

#[no_mangle]
pub extern "C" fn printHello() {
    println!("Hello! (This was printed in Rust)");
}

#[no_mangle]
pub extern "C" fn hello() -> *const c_char {
    return to_ptr("Hello from Rust!".to_string());
}

#[no_mangle]
pub extern "C" fn formatHello(str: *const c_char) -> *const c_char {
    let argAsString = to_string(str);
    return to_ptr(format!(
        "Hello {}, Rust is saying hello to you!",
        argAsString
    ));
}

#[no_mangle]
pub extern "C" fn processJson(string: *const c_char) -> *const c_char {
    let message: JsonMessage = serde_json::from_str(&to_string(string)).unwrap();
    let responseJson = serde_json::to_string(&JsonMessage {
        message: format!("Echoing the message [{}]", message.message),
    }).unwrap();
    return to_ptr(responseJson);
}

#[derive(Serialize, Deserialize)]
struct JsonMessage {
    message: String,
}

//pub mod db;
pub mod purepg;

#[no_mangle]
pub extern "C" fn readFromDb(query: *const c_char) -> *const c_char {
    //    let connection = db::establish_connection();
    //    let posts = db::get_posts_diesel(connection);
    let posts = purepg::get_posts();
    return to_ptr(posts.to_string());
}

// #[no_mangle]
//
// pub extern "C" fn sqlQuery(query: *const c_char, params: *const c_char) -> *const c_char {
//     let queryString = to_string(query);
//     let paramsString = to_string(params);
//     let params = purepg::toGcValues(&paramsString).expect(&*format!(
//         "could not convert gc values successfully: {}",
//         &paramsString
//     ));
//     let posts = purepg::query(queryString, params.iter().collect());
//     return to_ptr(posts.to_string());
// }

#[no_mangle]
pub extern "C" fn newCounterByReference() -> Box<Counter> {
    let c = Counter { count: 0 };
    //    mem::forget(&c);
    return Box::new(c);
}

#[no_mangle]
pub extern "C" fn newCounterByValue() -> Counter {
    // i added this public function so that the Counter struct is added to the header file. Otherwise it won't.
    let c = Counter { count: 0 };
    //    mem::forget(&c);
    return c;
}

#[no_mangle]
#[repr(C)]
pub struct Counter {
    count: u64,
}

impl Drop for Counter {
    fn drop(&mut self) {
        println!("Dropping!"); // i would like to see this getting called to ensure we don't leak memory
    }
}

#[no_mangle]
pub extern "C" fn increment(arg: &mut Counter) {
    arg.count = arg.count + 1
    //println!(format!("count is {} now!", self.count))
}

/// Convert a native string to a Rust string
fn to_string(pointer: *const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap().to_string()
}

/// Convert a Rust string to a native string
fn to_ptr(string: String) -> *const c_char {
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.

    mem::forget(cs);
    ptr
}
