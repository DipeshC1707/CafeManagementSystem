package com.inn.cafe.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;


import com.inn.cafe.JWT.CustomerUserDetailsService;
import com.inn.cafe.JWT.JwtFilter;
import com.inn.cafe.JWT.JwtUtil;
import com.inn.cafe.POJO.Token;
import com.inn.cafe.POJO.User;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.dao.TokenDao;
import com.inn.cafe.dao.UserDao;
import com.inn.cafe.service.UserService;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.utils.EmailUtils;
import com.inn.cafe.wrapper.UserWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;
    
    @Autowired
    TokenDao tokenDao;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signUp {}", requestMap);
        try {
            if (validateSignUpMap(requestMap)) {
                User user = userDao.findByEmailId(requestMap.get("email"));
                if (Objects.isNull(user)) {
                    userDao.save(getUserFromMap(requestMap));
                    return CafeUtils.getResponseEntity("Successfully Registered", HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity("Email Already Exists", HttpStatus.BAD_REQUEST);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUpMap(Map<String, String> requestMap) {
        if (requestMap.containsKey("name") && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email") && requestMap.containsKey("password")) {
            return true;
        } else {
            return false;
        }
    }

    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(passwordEncoder.encode(requestMap.get("password")));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setRole("user");
        user.setStatus("false");
        return user;
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside Login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password")));
            if (auth.isAuthenticated()) {
                if (customerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")) {
                    return new ResponseEntity<String>(
                            "{\"token\":\"" + jwtUtil.generateToken(customerUserDetailsService.getUserDetail().getEmail(),
                                    customerUserDetailsService.getUserDetail().getRole()) + "\"}",
                            HttpStatus.OK);
                } else {
                    return new ResponseEntity<String>("{\"message\":\"" + "Wait for Admin Approval." + "\"}",
                            HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            log.error("{}", e);
        }
        return new ResponseEntity<String>("{\"message\":\"" + "Bad Credentials." + "\"}", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
       try {
            if(jwtFilter.isAdmin())
            {
                return new ResponseEntity<>(userDao.getAllUser(),HttpStatus.OK);
            }
            else{
                return new ResponseEntity<List<UserWrapper>>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
       } catch (Exception e) {
            e.printStackTrace();
       }

       return new ResponseEntity<>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if(jwtFilter.isAdmin())
            {
                Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get("id")));

                if(!optional.isEmpty())
                {
                    userDao.updateStatus(requestMap.get("status"),Integer.parseInt(requestMap.get("id")));
                    sendMailToAllAdmin(requestMap.get("status"),optional.get().getEmail(),userDao.getAllAdmin());
                    return CafeUtils.getResponseEntity("User Status updated", HttpStatus.OK);
                }
                else{
                    return CafeUtils.getResponseEntity("User ID doesn't exist.", HttpStatus.OK);
                }


            }
            else
            {
                CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if(status!=null && status.equalsIgnoreCase("true"))
        {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Approved", "USER:-"+user+"\n is approved by \nAdmin-"+jwtFilter.getCurrentUser(), allAdmin);
        }
        else{
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Disabled", "USER:-"+user+"\n is disabled by \nAdmin-"+jwtFilter.getCurrentUser(), allAdmin);
        }
    }

    @Override
    public ResponseEntity<String> checkToken() {
       return CafeUtils.getResponseEntity("true",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            Optional<User> userObj = userDao.findByEmail(jwtFilter.getCurrentUser());
            // log.info(userObj.toString());
            if(userObj.isPresent())
            {
                User user = userObj.get();
                if(passwordEncoder.matches(requestMap.get("oldPassword"),user.getPassword()))
                {
                    user.setPassword(passwordEncoder.encode(requestMap.get("newPassword")));
                    userDao.save(user);
                    return CafeUtils.getResponseEntity("Password Updated Successfully", HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgetPassword(Map<String, String> requestMap) {
        try {
            String email = requestMap.get("email");
            Optional <User> userOptional = userDao.findByEmail(email);

        if (userOptional.isPresent()) {
        User user = userOptional.get();
        Optional<Token> existingToken = tokenDao.findByUserAndExpirationDateAfter(user, Instant.now());

        if (existingToken.isPresent()) {
            return CafeUtils.getResponseEntity("A valid token already exists. Please check your email.", HttpStatus.BAD_REQUEST);
        }

        String token = UUID.randomUUID().toString();

        Token resetToken = new Token();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpirationDate(Instant.now().atZone(ZoneId.of("Asia/Kolkata")).toInstant().plus(1, ChronoUnit.HOURS));

        tokenDao.save(resetToken);

        emailUtils.forgotMail(email, token);

        return CafeUtils.getResponseEntity("Request for change password is sent.Check Mail.", HttpStatus.OK);
            
        }
        return CafeUtils.getResponseEntity("User Not Found",HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> changePasswordToken(Map<String, String> requestMap) {
        try {
        String token = requestMap.get("token");
        String newPassword = requestMap.get("password");

        Optional<Token> tokenOptional = tokenDao.findByToken(token);

        if (!tokenOptional.isPresent() || tokenOptional.get().getExpirationDate().isBefore(Instant.now())) {
            return CafeUtils.getResponseEntity("Invalid or Expired Token", HttpStatus.BAD_REQUEST);
        }

        User user = tokenOptional.get().getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        userDao.save(user);
        tokenDao.delete(tokenOptional.get());

        return CafeUtils.getResponseEntity("Password reset successfully", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // @Override
    // public ResponseEntity<String> forgetPassword(Map<String, String> requestMap) {
    //     try {
    //         User user = userDao.findByEmail(requestMap.get("email"));
    //         if(!Objects.isNull(user) && !Strings.isNullOrEmpty(user.getEmail()))
    //         {
    //             emailUtils.forgotMail(user.getEmail(),"Credentials by Cafe Management System",user.getPassword());
    //             return CafeUtils.getResponseEntity("Check Your Mail for Credentials", HttpStatus.OK);
    //         }
    //     } catch (Exception e) {
    //         log.info(e.toString());
    //         e.printStackTrace();
    //     }
    //     return CafeUtils.getResponseEntity("Went Wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    // }
}
