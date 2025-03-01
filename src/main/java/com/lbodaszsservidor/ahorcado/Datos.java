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
        setPalabraOculta("polla de mono");
        setPalabraAdivinar(ocultarPalabra());
        setLetrasProbadas(new ArrayList<>());
        setNumeroIntentos(1);
        setIntentosRestantes(7);
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
        final String API_URL = "http://localhost:9090/palabra-random";

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(API_URL, String.class);
    }

    public String ocultarPalabra(){

        String palabraOculta = getPalabraOculta();

// Dividimos por espacios y reemplazamos cada palabra por "_ "
        String[] palabras = palabraOculta.split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            resultado.append("_ ".repeat(palabra.length())).append("  "); // Espacio extra entre palabras
        }

// Eliminamos el último espacio extra al final y retornamos
        return resultado.toString().trim();


    }
}
