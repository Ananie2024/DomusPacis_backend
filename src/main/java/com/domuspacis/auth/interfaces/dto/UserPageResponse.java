// UserPageResponse.java
package com.domuspacis.auth.interfaces.dto;
import java.util.List;

public record UserPageResponse(
        List<UserResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last
) {}