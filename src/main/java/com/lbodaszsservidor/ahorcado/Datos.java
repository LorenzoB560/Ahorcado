package com.lbodaszsservidor.ahorcado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@AllArgsConstructor
@Data
public class Datos {

    public Datos(){
        setPalabraOculta(devolverPalabra());
        setPalabraAdivinar("_ ".repeat(getPalabraOculta().length()));
        setLetrasProbadas(new ArrayList<>());
        setNumeroIntentos(0);
        setIntentosRestantes(6);
    }

    private String palabraOculta;
    private String palabraAdivinar;

    @NotNull(message = "La letra no puede ser nula")
    @Size(min = 1, max = 1, message = "La letra solo puede ser de un caracter")
    private String ultimaLetra;

    private ArrayList<String> letrasProbadas;
    private int numeroIntentos;
    private int intentosRestantes;

    public String devolverPalabra(){
        final String API_URL = "http://localhost:9999/palabra-random";

        RestTemplate restTemplate = new RestTemplate();
        Palabra palabra = restTemplate.getForObject(API_URL, Palabra.class);
        assert palabra != null;
        return palabra.getPalabra();
    }


}
