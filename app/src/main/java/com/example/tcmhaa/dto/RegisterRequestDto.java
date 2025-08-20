package com.example.tcmhaa.dto;

public class RegisterRequestDto {
    public String email;
    public String password;
    public String name;        // 新增
    public String gender;      // 新增：請用「男生 / 女生」
    public String birth_date;  // 新增：格式 YYYY/MM/DD
    public RegisterRequestDto( String email, String password, String name, String gender, String birth_date) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birth_date = birth_date;
    }
}
