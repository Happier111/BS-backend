package com.srx.transaction.Controller;

import com.google.gson.Gson;
import com.srx.transaction.Entities.BusinessUser;
import com.srx.transaction.Entities.CommonUser;
import com.srx.transaction.Entities.DTO.ResultMessage;
import com.srx.transaction.Entities.User;
import com.srx.transaction.Serivce.Impl.UserServiceImplement;
import com.srx.transaction.Util.CodeUtil;
import com.srx.transaction.Util.PaginationUtil;
import com.srx.transaction.Util.PictureUtil;
import com.srx.transaction.Util.ValidationCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static com.srx.transaction.Enum.ResultCode.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserServiceImplement userService;

    @PostMapping("/login")
    public ResultMessage login(@RequestBody User user, HttpServletRequest session) {
        if (user != null) {
            String role = user.getRole();
            String username = user.getUsername();
            String password = user.getPassword();
            if (username != null && password != null) {
                User login = userService.login(username, password);
                if (login != null) {
                    session.getSession().setAttribute("user", login);
                    if (role.equals("0")) {
                        CommonUser commonUserById = userService.getCommonUserById(login.getUserId());
                        return new ResultMessage(LOGIN_SUCCESS, commonUserById);
                    } else if (role.equals("1")) {
                        BusinessUser businessUser = userService.getBusinessUserById(login.getUserId());
                        return new ResultMessage(LOGIN_SUCCESS, businessUser);
                    } else {
                        return new ResultMessage(LOGIN_SUCCESS, login);
                    }
                }
                return new ResultMessage(ERROR_NOFOUND_USER);
            }
            return new ResultMessage(ERROR_NOLOGIN);
        }
        return new ResultMessage(ERROR_NOLOGIN);
    }

    /**
     * 图片验证码测试类
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @GetMapping("/getCode")
    public void createImg(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
            response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expire", 0);
            ValidationCodeUtil randomValidateCode = new ValidationCodeUtil();
            randomValidateCode.getRandcode(request, response);//    输出验证码图片
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/validationCode")
    public ResultMessage validationCode(HttpServletRequest request, @RequestParam String code) {
        String key = CodeUtil.codeUpper((String) request.getSession().getAttribute("RANDOMREDISKEY"));
        System.out.println(code);
        System.out.println(key);
        if (key.equals(code)) {
            return new ResultMessage(VALIDATION_SUCCESS);
        } else {
            return new ResultMessage(FAIL_VALIDATION);
        }
    }


    @PostMapping("/registerCommon")
    public ResultMessage registerCommon(@RequestPart("user") String user, @RequestPart("commonUser") String commonuser) {
        Gson gson = new Gson();
        User baseUser = gson.fromJson(user, User.class);
        CommonUser commonUser = gson.fromJson(commonuser, CommonUser.class);
        if (commonUser != null && baseUser != null) {
            User user1 = new User();
            user1.setUsername(baseUser.getUsername());
            user1.setPassword(CodeUtil.get_MD5_code(baseUser.getPassword()));
            user1.setRole(baseUser.getRole());
            user1.setEmail(baseUser.getEmail());
            Boolean register = userService.registerCommonUser(baseUser, commonUser);
            if (register)
                return new ResultMessage(REGISTER_SUCCESS, register);
            else return new ResultMessage(REGISTER_FAIL, register);
        } else return new ResultMessage(REGISTER_FAIL, false);
    }

    @PostMapping("/registerBusiness")
    public ResultMessage registerBusiness(@RequestPart("user") String user, @RequestPart("businessUser") String businessuser,
                                          @RequestPart("licence") MultipartFile license, @RequestPart("identityFront") MultipartFile identityFront,
                                          @RequestPart("identityBack") MultipartFile identityBack) throws Exception {
        Gson gson = new Gson();
        User baseUser = gson.fromJson(user, User.class);
        BusinessUser businessUser = gson.fromJson(businessuser, BusinessUser.class);
        String shopUUID = CodeUtil.get_uuid();
        String licenseUrl = "" ;
        String identityFrontUrl = "" ;
        String identityBackUrl = "" ;
        Boolean flag = PictureUtil.uploadPicture(license, shopUUID, null, PictureUtil.LICENSE);
        Boolean flag1 = PictureUtil.uploadPicture(identityFront, shopUUID, null, PictureUtil.IDENTITY_FRONT);
        Boolean flag2 = PictureUtil.uploadPicture(identityBack, shopUUID, null, PictureUtil.IDENTITY_BACK);
        if (flag) {
            licenseUrl = PictureUtil.getUrl(shopUUID, null, PictureUtil.LICENSE);
            businessUser.setLicense(licenseUrl);
        }
        if (flag1) {
            identityFrontUrl = PictureUtil.getUrl(shopUUID, null, PictureUtil.IDENTITY_FRONT);
            businessUser.setIdentificationFront(identityFrontUrl);
        }
        if (flag2) {
            identityBackUrl = PictureUtil.getUrl(shopUUID, null, PictureUtil.IDENTITY_BACK);
            businessUser.setIdentificationBack(identityBackUrl);
        }
        if (businessUser != null && baseUser != null) {
            User user1 = new User();
            user1.setUsername(baseUser.getUsername());
            user1.setPassword(CodeUtil.get_MD5_code(baseUser.getPassword()));
            user1.setRole(baseUser.getRole());
            user1.setEmail(baseUser.getEmail());
            Boolean register = userService.registerBusinessUser(baseUser, businessUser, shopUUID);
            if (register)
                return new ResultMessage(REGISTER_SUCCESS, register);
            else return new ResultMessage(REGISTER_FAIL, register);
        } else return new ResultMessage(REGISTER_FAIL, false);
    }

    @GetMapping("/getUserById")
    public ResultMessage getUserById(@RequestParam String userId) {
        CommonUser commonUserById = userService.getCommonUserById(userId);
        BusinessUser businessUser = userService.getBusinessUserById(userId);
        if (commonUserById != null) {
            return new ResultMessage(DATA_RETURN_SUCCESS, commonUserById);
        } else if (businessUser != null) {
            return new ResultMessage(DATA_RETURN_SUCCESS, businessUser);
        } else {
            return new ResultMessage(ERROR_NO_DATA);
        }
    }

    /**
     * @param mode        选择查询的用户角色，1为商家用户，0位普通用户
     * @param currentPage
     * @param pageSize
     * @param status      选择查询用户的状态，0为正常用户，-1为未批准用户，1为异常账号
     * @return
     */
    @GetMapping("/getUserList")
    public ResultMessage getUserList(@RequestParam String mode, @RequestParam Integer currentPage,
                                     @RequestParam Integer pageSize, @RequestParam(required = false) String status) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (mode.equals("0")) {
            ResultMessage paginationResult = PaginationUtil.getPaginationResult(currentPage, pageSize, userService, "getCommonUserList", status);
            return paginationResult;
        } else if (mode.equals("1")) {
            ResultMessage paginationResult = PaginationUtil.getPaginationResult(currentPage, pageSize, userService, "getBusinessUserList", status);
            return paginationResult;
        } else
            return new ResultMessage(ERROR_ROLE);
    }

    /**
     * 该接口是给管理员用户批准或，封禁用户账号时使用
     * 用户注册后，账号的状态为待批准，需要调用此接口批准用户创建账号的申请
     * 用户有违规行为后，也可使用该接口对用户进行封禁
     *
     * @param username
     * @param status
     * @return
     */
    @GetMapping("/changeStatus")
    public ResultMessage changeStatus(@RequestParam String username, @RequestParam String status) {
        Boolean flag = userService.updateUserStatus(username, status);
        if (flag) {
            return new ResultMessage(UPDATE_USER_STATUS_SUCCESS);
        }
        return new ResultMessage(UPDATE_USER_STATUS_FAIL);
    }

    @PostMapping("/updatePassword")
    public ResultMessage updatePassword(@RequestParam String email, @RequestParam String oldPassword,
                                        @RequestParam String newPassword) {
        Boolean flag = userService.updatePassword(email, oldPassword, newPassword);
        if (flag) {
            return new ResultMessage(UPDATE_PASSWORD_SUCCESS);
        }
        return new ResultMessage(UPDATE_PASSWORD_FAIL);

    }

}