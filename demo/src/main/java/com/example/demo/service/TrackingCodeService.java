package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrackingCodeService {
    private static final String PREFIX = "AA";
    private static final AtomicLong sequence = new AtomicLong(100000000);

    public String gerarCodigoRastreio() {
        long numero = sequence.getAndIncrement();
        int[] digitos = new int[8];
        String numeroStr = String.format("%08d", numero);

        for (int i = 0; i < 8; i++) {
            digitos[i] = numeroStr.charAt(i) - '0';
        }

        int[] sequencia = {8, 6, 4, 2, 3, 5, 9, 7};
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += digitos[i] * sequencia[i];
        }

        int resto = soma % 11;
        int checksum1 = (resto == 0) ? 5 : (resto == 1) ? 0 : 11 - resto;
        int checksum2 = (int)(numero % 10);

        return PREFIX + numeroStr + checksum1 + checksum2 + "BR";
    }
}
