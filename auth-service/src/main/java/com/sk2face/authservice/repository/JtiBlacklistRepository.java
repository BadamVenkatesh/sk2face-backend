package com.sk2face.authservice.repository;

import com.sk2face.authservice.entity.JtiBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JtiBlacklistRepository extends JpaRepository<JtiBlacklist, String> {

}
