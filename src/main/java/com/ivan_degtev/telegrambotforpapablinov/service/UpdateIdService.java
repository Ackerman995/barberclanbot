package com.ivan_degtev.telegrambotforpapablinov.service;

import org.springframework.stereotype.Service;

@Service
public interface UpdateIdService {

    boolean isUniqueUpdateId(Long updateId);

}
