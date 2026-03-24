package com.collaboration.model;

import java.sql.Timestamp;

public class Answer {
    private Long id;
    private Long questionId;
    private Long userId;
    private String answerText;
    private Integer votes;
    private Timestamp createdAt;
    
    private String username;

    public Answer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    
    public Integer getVotes() { return votes; }
    public void setVotes(Integer votes) { this.votes = votes; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
