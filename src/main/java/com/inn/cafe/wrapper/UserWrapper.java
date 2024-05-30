package com.inn.cafe.wrapper;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserWrapper {
    
    private  Integer id;

    private String username;

    private String email;

    private String contactNumber;

    private String status;

    public UserWrapper(Integer id, String username, String email, String contactNumber, String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.contactNumber = contactNumber;
        this.status = status;
    }

    
}
