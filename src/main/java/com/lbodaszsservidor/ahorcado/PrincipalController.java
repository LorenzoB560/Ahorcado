package com.lbodaszsservidor.ahorcado;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.coyote.Request;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

@Controller
public class PrincipalController {

    @GetMapping("/ahorcado")
    public String devuelveAhorcado(HttpSession session, Model model) {

        Datos datos = (Datos) session.getAttribute("datos");
        if (datos == null) {
            datos = new Datos();
            session.setAttribute("datos", datos);
        }
        model.addAttribute("datos", datos);
        return "ahorcado";
    }

    @PostMapping("/guardar-ahorcado")
    public String guardarAhorcado(@Valid @ModelAttribute Datos datosFormulario,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model) {

        Datos datosSesion = (Datos) session.getAttribute("datos");
        if (datosSesion == null) {
            return "redirect:/ahorcado";
        }

        if (bindingResult.hasErrors()) {
            actualizarDatos(datosSesion, datosFormulario);
            datosFormulario.setNumeroIntentos(datosSesion.getNumeroIntentos());
            return "ahorcado";
        }


        if (datosSesion.getLetrasProbadas().contains(datosFormulario.getUltimaLetra().toLowerCase())) {
            model.addAttribute("letraRepetida", "Esta letra ya ha sido introducida. Elija otra.");
            actualizarDatos(datosSesion, datosFormulario);

            return "ahorcado";
        }


        System.out.println("Datos Formulario: " + datosFormulario);
        System.out.println("Datos Sesion: " + datosSesion);

        actualizarDatos(datosSesion, datosFormulario);

        actualizarIntentos(datosSesion, datosFormulario);


        if (unirPalabra(datosFormulario).equals(datosFormulario.getPalabraOculta())){
            String mensajeVictoria = "¡Enhorabuena! ¡Has ganado! ¿Quieres volver a jugar?";
            model.addAttribute("mensajeVictoria", mensajeVictoria);
            return "ahorcado";
        }

        //Evitar que se repinte la ultima letra puesta en el campo de texto
        datosFormulario.setUltimaLetra("");


        session.setAttribute("datos", datosFormulario);
        return "redirect:/ahorcado";
    }
    private void actualizarDatos(Datos sesionVieja, Datos sesionNueva) {
        if (sesionVieja == null) {
            sesionVieja = new Datos(); // Se crea una nueva instancia si la sesión ha expirado
        }

        if (sesionNueva == null) {
            return; // Si no hay nuevos datos, no hacemos nada
        }
        sesionNueva.setPalabraOculta(sesionVieja.getPalabraOculta());

        if (sesionNueva.getUltimaLetra().length() == 1){
            sesionNueva.setPalabraAdivinar(construccionPalabra(sesionVieja.getPalabraOculta(), sesionVieja.getPalabraAdivinar(), sesionNueva.getUltimaLetra()));
        } else{
            sesionNueva.setPalabraAdivinar(sesionVieja.getPalabraAdivinar());
        }

        ArrayList<String> lista = sesionVieja.getLetrasProbadas();
        String letra = sesionNueva.getUltimaLetra().toLowerCase();
        if (!lista.contains(letra) && letra.length()==1) {
            lista.add(letra);
        }
        sesionNueva.setLetrasProbadas(lista);

        sesionNueva.setNumeroIntentos(sesionVieja.getNumeroIntentos());
        sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes());

    }
    public void actualizarIntentos(Datos sesionVieja, Datos sesionNueva) {
        int intentos = sesionVieja.getNumeroIntentos();
        sesionNueva.setNumeroIntentos(intentos >= 0 ? intentos + 1 : intentos);


        if (!sesionVieja.getPalabraOculta().contains(sesionNueva.getUltimaLetra().toLowerCase())) {
            sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes() >= 0 ? sesionVieja.getIntentosRestantes() - 1 : intentos);
        } else{
            sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes());
        }
    }


    public static String construccionPalabra(String palabraOculta, String palabraAdivinar, String letra) {
        if (letra == null || letra.isEmpty()) {
            return palabraAdivinar; // Si no hay letra, devolvemos la palabra tal cual está
        }

        // Normalizar las letras (sin importar mayúsculas, tildes o diéresis)
        String[][] equivalencias = {
                {"a", "á"}, {"e", "é"}, {"i", "í"}, {"o", "ó"}, {"u", "ú", "ü"}
        };

        String letraMinus = letra.toLowerCase();
        char[] palabraLetras = palabraOculta.toCharArray();
        char[] palabraActual = palabraAdivinar.toCharArray();

        for (int i = 0; i < palabraLetras.length; i++) {
            char letraPalabra = Character.toLowerCase(palabraLetras[i]);

            // Comprobamos si la letra coincide directamente
            boolean coincide = letraPalabra == letraMinus.charAt(0);

            // Si no coincide directamente, verificamos equivalencias
            if (!coincide) {
                for (String[] grupo : equivalencias) {
                    if (Arrays.asList(grupo).contains(letraMinus) && Arrays.asList(grupo).contains(String.valueOf(letraPalabra))) {
                        coincide = true;
                        break;
                    }
                }
            }

            // Si coincide, reemplazamos la letra en la palabra adivinada
            if (coincide) {
                palabraActual[i * 2] = palabraLetras[i]; // Respetamos la mayúscula original
            }
        }

        return new String(palabraActual);
    }

    public static String unirPalabra(Datos datosFormulario){
        char[] palabraDividida = datosFormulario.getPalabraAdivinar().toCharArray();
        return new String(palabraDividida).replaceAll(" ", "");
    }
}
