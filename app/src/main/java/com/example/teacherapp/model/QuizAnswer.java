package com.example.teacherapp.model;

import java.util.List;

public class QuizAnswer {
    private String questionId;
    private String type;
    private String promptHtml;
    private String displayAnswer;
    private Boolean isCorrect;
    private String correctAnswer;
    private List<Integer> selectedOptions;
    private List<String> fibAnswers;
    private Boolean trueFalseAnswer;
    private String shortAnswer;

    public QuizAnswer() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPromptHtml() {
        return promptHtml;
    }

    public void setPromptHtml(String promptHtml) {
        this.promptHtml = promptHtml;
    }

    public String getDisplayAnswer() {
        return displayAnswer;
    }

    public void setDisplayAnswer(String displayAnswer) {
        this.displayAnswer = displayAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public List<Integer> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<Integer> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    public List<String> getFibAnswers() {
        return fibAnswers;
    }

    public void setFibAnswers(List<String> fibAnswers) {
        this.fibAnswers = fibAnswers;
    }

    public Boolean getTrueFalseAnswer() {
        return trueFalseAnswer;
    }

    public void setTrueFalseAnswer(Boolean trueFalseAnswer) {
        this.trueFalseAnswer = trueFalseAnswer;
    }

    public String getShortAnswer() {
        return shortAnswer;
    }

    public void setShortAnswer(String shortAnswer) {
        this.shortAnswer = shortAnswer;
    }
}
