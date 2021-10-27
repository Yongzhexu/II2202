package com.example.test_location.models;

public class QueryType {
    private static QueryType instance = new QueryType();
    private String queryType = "05";
    private QueryType(){}

    public static QueryType getInstance() {
        return instance;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getQueryTypeText(){
        switch(queryType){
            case "05":
                return "restaurant";
            case "06":
                return "supermarket";
            case "09":
                return "medical";
            default:
                return "";
        }
    }

    public void setQueryType(String queryType) {
        System.out.println(queryType);
        switch (queryType) {
            case "restaurant":
                this.queryType = "05";
                break;
            case "medical":
                this.queryType = "09";
                break;
            case "supermarket":
                this.queryType = "06";
                break;
        }
    }
}
