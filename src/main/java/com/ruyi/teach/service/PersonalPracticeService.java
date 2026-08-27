package com.ruyi.teach.service;

import com.ruyi.teach.model.dto.PersonalPracticeCreateRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.PersonalPracticeCreateVO;

public interface PersonalPracticeService {
    PersonalPracticeCreateVO create(PersonalPracticeCreateRequest request, User student);
}
