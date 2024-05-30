package com.inn.cafe.service;

import org.springframework.http.ResponseEntity;

import com.inn.cafe.wrapper.UserWrapper;

import java.util.*;

public interface UserService {
    ResponseEntity<String> signUp(Map<String, String> requestMap);

    ResponseEntity<String> login(Map<String, String> requestMap);

    ResponseEntity<List<UserWrapper>> getAllUser();

    ResponseEntity<String> update(Map<String, String> requestMap);

    ResponseEntity<String> checkToken();

    ResponseEntity<String>changePassword(Map<String,String>requestMap);
    
    ResponseEntity<String>changePasswordToken(Map<String,String>requestMap);

    ResponseEntity<String>forgetPassword(Map<String,String>requestMap);



}
