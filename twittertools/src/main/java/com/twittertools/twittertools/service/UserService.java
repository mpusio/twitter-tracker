package com.twittertools.twittertools.service;

import com.twittertools.twittertools.entity.User;
import com.twittertools.twittertools.json.UserMapper;
import com.twittertools.twittertools.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers(){
        return userRepository.findAll();
    }

    public List<User> getUsers(Optional<Integer> from, Optional<Integer> to){
        if (from.isEmpty() && to.isEmpty()) return userRepository.findAll();
        if (from.isPresent() && to.isEmpty()) return userRepository.findAll().subList(from.get(), userRepository.findAll().size());
        if (from.isEmpty() && to.isPresent()) return userRepository.findAll().subList(from.get(), userRepository.findAll().size());
        if (from.isPresent() && to.isPresent()) return userRepository.findAll().subList(from.get(), to.get());
        return null;
    }

    public void saveUsers(List<UserMapper> usersMap){
        List<User> users = usersMap.stream()
                .map(UserMapper::mapToUser)
                .collect(Collectors.toList());

        for (User user : users) {
            userRepository.save(user);
        }
    }

    public void saveUser(User user){
        userRepository.save(user);
    };

    public void updateUser(String id, List<UserMapper> usersMap){
        User user = userRepository.findById(id).get();
        List<User> following = usersMap.stream()
                .map(UserMapper::mapToUser)
                .collect(Collectors.toList());

        user.setUsers(following);

        userRepository.save(user);
    }

    public User findById(String id){
        return userRepository.findById(id).get();
    }

    public void generateReportPDF(){}

    public void track(){
        // http request/requests in controller
        // get json string format and parse like i want

        // save to database
        // make pdf raport
    }

    public void jsonParse(){

    }

}
