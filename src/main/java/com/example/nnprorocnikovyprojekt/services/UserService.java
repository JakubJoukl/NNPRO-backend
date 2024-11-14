package com.example.nnprorocnikovyprojekt.services;


import com.example.nnprorocnikovyprojekt.Utility.Utils;
import com.example.nnprorocnikovyprojekt.dtos.general.GeneralResponseDto;
import com.example.nnprorocnikovyprojekt.dtos.pageinfo.PageInfoDtoResponse;
import com.example.nnprorocnikovyprojekt.dtos.pageinfo.PageInfoRequestWrapper;
import com.example.nnprorocnikovyprojekt.dtos.user.*;
import com.example.nnprorocnikovyprojekt.entity.*;
import com.example.nnprorocnikovyprojekt.external.CaptchaService;
import com.example.nnprorocnikovyprojekt.repositories.ResetTokenRepository;
import com.example.nnprorocnikovyprojekt.repositories.UserRepository;
import com.example.nnprorocnikovyprojekt.repositories.VerificationCodeRepository;
import com.example.nnprorocnikovyprojekt.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SecureRandom secureRandom = new SecureRandom();
    private Integer RANDOM_BOUND = 999999;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.getUserByUsername(username).orElse(null);
    }

    public User getUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.getUserByUsername(username).orElse(null);
    }

    public User getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getUserByUsername(authentication.getName());
    }

    public ResetToken getResetTokenByValue(String resetTokenValue){
        return resetTokenRepository.getResetTokenByToken(resetTokenValue).orElseThrow(() -> new RuntimeException("Nebyl nalezen token"));
    }

    public void newPassword(NewPasswordDto resetPasswordRequest){
        ResetToken resetToken = getResetTokenByValue(resetPasswordRequest.getToken());

        if(resetToken == null) throw new RuntimeException("Reset token was not found");

        boolean resetTokenIsValid = resetToken.isValid() && LocalDateTime.now().isBefore(resetToken.getExpirationDate());
        if(resetTokenIsValid){
            User user = resetToken.getUser();
            changePassword(resetPasswordRequest.getPassword(), user);
            saveUser(user);
        }
        resetToken.setValid(false);
        saveResetToken(resetToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivateUserResetTokens(User user){
        user.getResetTokens().forEach(resetToken -> {
            resetToken.setValid(false);
            resetTokenRepository.save(resetToken);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivateUserVerificationTokens(User user){
        user.getVerificationCodes().forEach(verificationCode -> {
            verificationCode.setValid(false);
            verificationCodeRepository.save(verificationCode);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean registerUser(RegistrationDto registrationRequest) {
        boolean captchaIsValid = captchaService.validateCaptcha(registrationRequest.getCaptchaToken());
        if(!captchaIsValid) {
            throw new RuntimeException("Captcha is not valid");
        }

        boolean alreadyExists = userRepository.getUserByUsername(registrationRequest.getUsername()).isPresent();
        if(alreadyExists) return false;
        else return userRepository.save(new User(registrationRequest.getUsername(), encryptPassword(registrationRequest.getPassword()), registrationRequest.getEmail()))
                .getUserId() != null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String newPassword, User user) {
        user.setPassword(encryptPassword(newPassword));
        saveUser(user);
    }

    public String encryptPassword(String password){
        return passwordEncoder.encode(password);
    }

    public boolean userPasswordMatches(String password, User user){
        return passwordEncoder.matches(password, user.getPassword());
    }

    @Transactional(rollbackFor = Exception.class)
    public VerificationCode generateVerificationCodeForUser(User user){
        deactivateUserVerificationTokens(user);
        String verificationCodeValue = Integer.toString(secureRandom.nextInt(RANDOM_BOUND));
        verificationCodeValue = StringUtils.leftPad(verificationCodeValue, 6, "0");
        LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(5);
        VerificationCode verificationCode = new VerificationCode(verificationCodeValue, expirationDate, user);
        user.getVerificationCodes().add(verificationCode);
        userRepository.save(user);
        return verificationCode;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetToken generateResetTokenForUser(User user) {
        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        deactivateUserResetTokens(user);
        ResetToken resetToken = new ResetToken(user, token);
        user.getResetTokens().add(resetToken);
        saveResetToken(resetToken);
        return resetToken;
    }

    public void saveResetToken(ResetToken resetToken){
        resetTokenRepository.save(resetToken);
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean verifyVerificationCode(String username, String verificationCodeValue) {
        if(verificationCodeValue.length() < Integer.toString(RANDOM_BOUND).length()) return false;
        User user = getUserByUsername(username);
        if(user == null) return false;

        List<VerificationCode> verificationCode = verificationCodeRepository.findValidVerificationCodes(user, verificationCodeValue);
        LocalDateTime now = LocalDateTime.now();

        boolean nonExpiredValidCodeExists = verificationCode.stream().anyMatch(verificationCode1 -> verificationCode1.getExpirationDate().isAfter(now));
        if(nonExpiredValidCodeExists) {
            deactivateUserVerificationTokens(user);
            return true;
        }
        return false;
    }

    public void updateUser(UpdateUserDto updateUserDto) throws JsonProcessingException {
        User user = getUserFromContext();
        if(!userPasswordMatches(updateUserDto.getConfirmationPassword(), user)) {
            throw new RuntimeException("Password does not match");
        }
        if(updateUserDto.getEmail() != null || updateUserDto.getPassword() != null || updateUserDto.getPublicKey() != null){
            if(updateUserDto.getEmail() != null) user.setEmail(updateUserDto.getEmail());
            if(updateUserDto.getPassword() != null) user.setPassword(encryptPassword(updateUserDto.getPassword()));
            if(updateUserDto.getPublicKey() != null) {
                user.getActivePublicKey().ifPresent(publicKey -> publicKey.setValid(false));
                user.getPublicKeys().add(new PublicKey(objectMapper.writeValueAsString(updateUserDto.getPublicKey()), LocalDateTime.now(), true, user));
            }
            saveUser(user);
        }
    }

    public void addContact(AddContactDto addContactDto) {
        User user = getUserFromContext();
        User contact = getUserByUsername(addContactDto.getUsername());

        if(contact == null) throw new RuntimeException("Contact is null");
        if(user.getContacts().contains(contact)) throw new RuntimeException("User has already added this contact.");

        user.getContacts().add(contact);
        saveUser(user);
    }

    public UserPageResponseDto listContacts(PageInfoRequestWrapper pageInfoRequestWrapper) {
        User user = getUserFromContext();
        List<User> contacts = Utils.getPage(user.getContacts(), pageInfoRequestWrapper.getPageIndex(), pageInfoRequestWrapper.getPageSize());
        return contactsToContactsPageResponseDtos(contacts, pageInfoRequestWrapper, user.getContacts().size());
    }

    private UserPageResponseDto contactsToContactsPageResponseDtos(List<User> contacts, PageInfoRequestWrapper pageInfoRequestWrapper, Integer total){
        List<UserDto> userDtos = contacts.stream()
                .map(this::userToUserDto)
                .collect(Collectors.toList());

        UserPageResponseDto userPageResponseDto = new UserPageResponseDto();
        userPageResponseDto.setItemList(userDtos);
        userPageResponseDto.setPageInfoDto(new PageInfoDtoResponse(pageInfoRequestWrapper.getPageSize(), pageInfoRequestWrapper.getPageIndex(), (long)total));
        return userPageResponseDto;
    }

    private UserDto userToUserDto(User user) {
        String publicKeyString = user.getActivePublicKey().isPresent()? user.getActivePublicKey().get().getKeyValue() : null;
        try {
            PublicKeyDto publicKeyDto;
            if(publicKeyString == null) {
                publicKeyDto = null;
            } else {
                publicKeyDto = objectMapper.readValue(publicKeyString, PublicKeyDto.class);
            }
            return new UserDto(user.getUsername(), user.getEmail(), publicKeyDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not parse key");
        }
    }

    private ContactDto userToContactDto(User user, User contact) {
        String publicKeyString = contact.getActivePublicKey().isPresent()? contact.getActivePublicKey().get().getKeyValue() : null;
        try {
            PublicKeyDto publicKeyDto;
            if(publicKeyString == null) {
                publicKeyDto = null;
            } else {
                publicKeyDto = objectMapper.readValue(publicKeyString, PublicKeyDto.class);
            }
            return new ContactDto(contact.getUsername(), contact.getEmail(), publicKeyDto, user.getContacts().contains(contact));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not parse key");
        }
    }

    public UserDto getUserData() {
        User user = getUserFromContext();
        return userToUserDto(user);
    }

    public ContactPageResponseDto searchUsers(SearchUserDtoRequest searchUserDtoRequest) {
        User user = getUserFromContext();
        Pageable pageInfo = PageRequest.of(searchUserDtoRequest.getPageInfo().getPageIndex(), searchUserDtoRequest.getPageInfo().getPageSize()).withSort(Sort.Direction.DESC, "userId");
        Page<User> usersPage = userRepository.findUsersByUsernameStartingWithAndUsernameNot(searchUserDtoRequest.getUsername(), user.getUsername(), pageInfo);
        return usersToUserPageResponseDto(usersPage, user);
    }

    private ContactPageResponseDto usersToUserPageResponseDto(Page<User> page, User user){
        if(page == null) return null;
        List<ContactDto> contactDtos = page.getContent().stream()
                .map(contact -> userToContactDto(user, contact)).toList();

        ContactPageResponseDto contactPageResponseDto = new ContactPageResponseDto();
        contactPageResponseDto.setItemList(contactDtos);
        contactPageResponseDto.setPageInfoDto(new PageInfoDtoResponse(page.getSize(), page.getSize(), page.getTotalElements()));
        return contactPageResponseDto;
    }

    public JwtTokenDto verify2FaAndGetJwtToken(VerificationDto verificationDto) {
        boolean captchaIsValid = captchaService.validateCaptcha(verificationDto.getCaptchaToken());
        if(!captchaIsValid) {
            throw new RuntimeException("Captcha is not valid");
        }

        boolean verificationCodeMatches = verifyVerificationCode(verificationDto.getUsername(), verificationDto.getVerificationCode());

        if(verificationCodeMatches) {
            String jwtToken = jwtService.generateToken(verificationDto.getUsername());
            JwtTokenDto jwtTokenDto = new JwtTokenDto();
            jwtTokenDto.setJwtToken(jwtToken);
            return jwtTokenDto;
        } else {
            throw new RuntimeException("Verification code does not match");
        }
    }

    public ExpirationDateDto loginUser(LoginDto authRequest) {
        boolean captchaIsValid = captchaService.validateCaptcha(authRequest.getCaptchaToken());
        if(!captchaIsValid) {
            throw new RuntimeException("Captcha is not valid");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            User user = getUserByUsername(authRequest.getUsername());
            VerificationCode verificationCode = emailService.sendVerificationCodeEmail(user);
            if(verificationCode != null) {
                ExpirationDateDto expirationDateDto = new ExpirationDateDto();
                expirationDateDto.setExpirationDate(verificationCode.getExpirationDate());
                return expirationDateDto;
            } else {
                throw new RuntimeException("Failed to send verification code");
            }
        } else {
            throw new RuntimeException("Failed to authenticate user");
        }
    }

    //TODO zkopirovane -> bude vubec potreba?
    /*
    public UserDto convertToDto(User user) {
        return modelMapper.map(user, UserDto.class);
    }

    public User convertToEntity(UserDto userDto) {
        return modelMapper.map(userDto, User.class);
    }
    */
}
