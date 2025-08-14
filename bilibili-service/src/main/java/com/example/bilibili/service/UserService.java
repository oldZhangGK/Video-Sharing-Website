package com.example.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import com.example.bilibili.dao.UserDao;
import com.example.bilibili.domain.PageResult;
import com.example.bilibili.domain.User;
import com.example.bilibili.domain.UserInfo;
import com.example.bilibili.domain.constant.UserConstant;
import com.example.bilibili.domain.exception.ConditionException;
import com.example.bilibili.service.util.MD5Util;
import com.example.bilibili.service.util.RSAUtil;
import com.example.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    //Transactional make this operation atomic which will either all success or all fail if one operation failed
    @Transactional
    public void addUser(User user){
        //Get user's phone number
        String phone = user.getPhone();
        if (StringUtils.isNullOrEmpty(phone)){
            throw new ConditionException("Phone number cannot be null");
        }

        //Check if the phone number get registered
        User dbUser = this.getUserByPhone(phone);
        if (dbUser != null){
            throw new ConditionException("Phone number already exists");
        }

        //Generate current timestamp and its salt
        Date now = new Date();
        String salt = String.valueOf(now.getTime());

        //get and decrypt the passwords from frontend
        String password = user.getPassword();
        String rawPassword;
        try{
            rawPassword = RSAUtil.decrypt(password);
        }
        catch (Exception e){
            throw new ConditionException("Password cannot be decrypted");
        }

        //encrypt rawpassword, save salt and md5 encrypted password for later access
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        user.setSalt(salt);
        user.setPassword(md5Password);
        user.setCreateTime(now);

        //add user to the user table
        userDao.addUser(user);

        //add user's info to user info table
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setNick(UserConstant.DEFAULT_NICK);
        userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
        userInfo.setGender(UserConstant.GENDER_MALE);
        userInfo.setCreateTime(now);
        userDao.addUserInfo(userInfo);
    }

    public User  getUserByPhone(String phone){
        return userDao.getUserByPhone(phone);
    }

    public User getUserInfo(Long userId) {
        User user = userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);
        user.setUserInfo(userInfo);
        return user;
    }

    public String login(User user) throws Exception{
        String phone = user.getPhone() == null ? "" : user.getPhone();
        String email = user.getEmail() == null ? "" : user.getEmail();

        //check if phone and email is empty
        if (StringUtils.isNullOrEmpty(phone) &&  StringUtils.isNullOrEmpty(email)){
            throw new ConditionException("Phone or email cannot be null");
        }

        //check if the user signed up in the database
        User dbUser = userDao.getUserByPhoneOrEmail(phone,email);
        if (dbUser == null){
            throw new ConditionException("User not found");
        }

        //decrypt the password from frontend
        String password =  user.getPassword();
        String rawPassword;
        try{
            rawPassword = RSAUtil.decrypt(password);
        }
        catch (Exception e){
            throw new ConditionException("Password cannot be decrypted");
        }

        //get salt and encrypt the password with the salt in MD5 method and compare if it is equal to the one stored in database
        String salt = dbUser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        if (!md5Password.equals(dbUser.getPassword())){
            throw new ConditionException("Passwords do not match");
        }

        //if the previous operations havent run into any of these exceptions. we would like to create a token according to its user's id for later authenticated login
        return TokenUtil.generateToken(dbUser.getId());

    }

    public void updateUsers(User user) throws Exception{
        Long id = user.getId();
        User dbUser = userDao.getUserById(id);
        if (dbUser == null){
            throw new ConditionException("User not found!");
        }

        //update password
        if (!StringUtils.isNullOrEmpty(user.getPassword())){
            String rawPassword = RSAUtil.decrypt(user.getPassword());
            String md5Password = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
            user.setPassword(md5Password);
        }
        user.setUpdateTime(new Date());
        userDao.updateUsers(user);
    }

    public void updateUserInfos(UserInfo userInfo){
        userInfo.setUpdateTime(new Date());
        userDao.updateUserInfos(userInfo);
    }

    public User getUserById(Long followingId) {
        return userDao.getUserById(followingId);
    }

    public List<UserInfo> getUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.getUserInfoByUserIds(userIdList);
    }

    public PageResult<UserInfo> pageListUserInfos(JSONObject params) {
        Integer no = params.getInteger("no");
        Integer size = params.getInteger("size");
        params.put("start", (no-1)*size); //从哪个INDEX开始，就是第几页开始
        params.put("limit", size);
        Integer total = userDao.pageCountUserInfos(params);
        List<UserInfo> list = new ArrayList<>();
        if (total > 0) {
            list = userDao.pageListUserInfos(params);
        }
        return new PageResult<>(total, list);
    }
}
