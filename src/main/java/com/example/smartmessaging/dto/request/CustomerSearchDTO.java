package com.example.smartmessaging.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerSearchDTO {

    private String name;
    private String phone;
    private String customerType;
    private List<String> customerTypes = new ArrayList<>();
    private String searchTag;
    private Boolean isRejectedOnly = false;
    private List<Long> tagIds = new ArrayList<>();
    private String matchType = "ANY";
    private String sortOrder = "latest";

    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 10), 100);
    }

    public int getTagCount() {
        return tagIds == null ? 0 : tagIds.size();
    }
}
