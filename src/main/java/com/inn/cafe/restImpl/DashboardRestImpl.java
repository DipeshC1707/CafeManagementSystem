package com.inn.cafe.restImpl;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.inn.cafe.rest.DashboardRest;
import com.inn.cafe.service.DashboardService;

@RestController
public class DashboardRestImpl implements DashboardRest{

    @Autowired
    DashboardService service;

    @Override
    public ResponseEntity<Map<String, Object>> getCount() {
        try {
            return service.getCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<Map<String,Object>>(new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
