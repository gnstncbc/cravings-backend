package com.gunes.cravings.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.gunes.cravings.dto.ParaStartRequestDTO;
import com.gunes.cravings.model.Para;
import com.gunes.cravings.repository.ParaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParaService {
    private final ParaRepository paraRepository;
    BigDecimal remainingMoney = BigDecimal.ZERO;

    public void setcreditCardRemainingLimit(String amount) {
        // Para miktarını BigDecimal olarak parse et
        BigDecimal amountBigDecimal = new BigDecimal(amount);

        // Para tablosundaki ilk kaydı güncelle
        paraRepository.findAll()
                .forEach(para -> {
                    para.setCreditCardRemainingLimit(amountBigDecimal.longValue());
                    paraRepository.save(para);
                });
    }

    public BigDecimal calculateRemainingMoney() {
        // Para miktarını BigDecimal olarak parse et

        paraRepository.findAll()
                .forEach(para -> {
                    // Para tablosundaki her bir kaydı işleyebilirsiniz
                    // Örneğin, para.getAmount() ile miktarı alabilirsiniz
                    BigDecimal totalLimitBigDecimal = BigDecimal.valueOf(para.getCreditCardTotalLimit());
                    BigDecimal remainingLimitBigDecimal = BigDecimal.valueOf(para.getCreditCardRemainingLimit());
                    BigDecimal monetSpentBigDecimal = totalLimitBigDecimal.subtract(remainingLimitBigDecimal);
                    BigDecimal salaryBigDecimal = BigDecimal.valueOf(para.getSalary());
                    remainingMoney = salaryBigDecimal.subtract(monetSpentBigDecimal);
                });

        return remainingMoney;
    }

    public void initParaTable(ParaStartRequestDTO paraInitRequestDTO) {
        // Yeni bir Para nesnesi oluştur ve veritabanına kaydet
        Para para = new Para();
        para.setCreditCardTotalLimit(paraInitRequestDTO.getCreditCardTotalLimit());
        para.setCreditCardRemainingLimit(paraInitRequestDTO.getCreditCardRemainingLimit());
        para.setSalary(paraInitRequestDTO.getSalary());

        // Para nesnesini kaydet
        paraRepository.save(para);
    }

}
