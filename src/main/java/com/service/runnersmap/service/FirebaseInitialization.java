package com.service.runnersmap.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;


public class FirebaseInitialization {

//  @Value("${firebase.key-path}")
//  private String FIREBASE_ACCOUNT_JSON;
//
//  @PostConstruct
//  public void initialize() {
//    try {
//      FileInputStream serviceAccount = new FileInputStream(FIREBASE_ACCOUNT_JSON);
//      FirebaseOptions options = new FirebaseOptions.Builder()
//          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//          .build();
//      FirebaseApp.initializeApp(options);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }

}
