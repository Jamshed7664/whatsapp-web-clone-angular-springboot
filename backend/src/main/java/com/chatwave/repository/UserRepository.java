package com.chatwave.repository;
import com.chatwave.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<AppUser,Long>{Optional<AppUser> findByEmail(String email); List<AppUser> findTop50ByNameContainingIgnoreCaseAndIdNotOrderByNameAsc(String name,Long id);}
