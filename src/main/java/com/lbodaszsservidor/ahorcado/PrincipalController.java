package com.lbodaszsservidor.ahorcado;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

@Controller
public class PrincipalController {

    @GetMapping("/ahorcado")
    public String devuelveAhorcado(HttpSession session, Model model) {

        //Creo la sesión si no hay, y posteriormente la añado al modelo.
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
        //Obtengo los datos de la sesión, y si no existe sesión, lo redirijo a /ahorcado
        // para crearla
        if (datosSesion == null) {
            return "redirect:/ahorcado";
        }

        //Comprobración de que el input sólo tenga una letra
        if (bindingResult.hasErrors()) {
            actualizarDatos(datosSesion, datosFormulario);
            datosFormulario.setNumeroIntentos(datosSesion.getNumeroIntentos());
            return "ahorcado";
        }

        //Comprobración de si se ha puesto una letra por segunda vez. Mira en la lista si la contiene,
        // obtieniendo la letra de datos formulario, pasándola a lower case y quitándole los acentos.
        if (datosSesion.getLetrasProbadas().contains(quitarAcentos(datosFormulario.getUltimaLetra().toLowerCase()))) {
            model.addAttribute("letraRepetida", "Esta letra ya ha sido introducida. Elija otra.");
            actualizarDatos(datosSesion, datosFormulario);
            return "ahorcado";
        }

        //Compruebo de que sea una letra válida, y no otro tipo de carácter.
        if (!validarLetra(datosFormulario.getUltimaLetra())) {
            model.addAttribute("formatoIncorrecto", "Valor introducido no válido");
            actualizarDatos(datosSesion, datosFormulario);
            return "ahorcado";
        }

        //Actualizo los datos en en la sesión nueva por los viejos, al igual que en los intentos.
        actualizarDatos(datosSesion, datosFormulario);
        actualizarIntentos(datosSesion, datosFormulario);


        //Mensaje de victoria
        if (unirPalabra(datosFormulario).equals(datosFormulario.getPalabraOculta())){
            String mensajeVictoria = "¡Enhorabuena! ¡Has ganado! ¿Quieres volver a jugar?";
            model.addAttribute("mensajeVictoria", mensajeVictoria);
            model.addAttribute("volverJugar", "Volver a jugar");
            return "ahorcado";
        } else if(datosFormulario.getIntentosRestantes() == 0){
            //Mensaje de derrota
            String mensajeDerrota = "Qué mal, has perdido. ¿Quieres volver a jugar?";
            model.addAttribute("mensajeDerrota", mensajeDerrota);
            model.addAttribute("volverJugar", "Volver a jugar");
            return "ahorcado";
        }


        //Vuelvo a guardar la sesión
        session.setAttribute("datos", datosFormulario);
        return "redirect:/ahorcado";
    }

    //Llego aquí después de darle al enlace de volver a jugar, para borrar la sesión y empezar a jugar desde cero
    @GetMapping("/volver-jugar")
    public String volverJugar(HttpSession session) {
        session.invalidate();
        return "redirect:/ahorcado";
    }

    private void actualizarDatos(Datos sesionVieja, Datos sesionNueva) {
        if (sesionVieja == null) {
            sesionVieja = new Datos(); // Se crea una nueva instancia si la sesión ha expirado
        }

        if (sesionNueva == null) {
            return; // Si no hay nuevos datos, no se hace nada
        }
        //Establezco la palabra oculta a adivinar
        sesionNueva.setPalabraOculta(sesionVieja.getPalabraOculta());

        //Voy rellenando las letras que se van adivinando de esta forma
        if (sesionNueva.getUltimaLetra().length() == 1){
            sesionNueva.setPalabraAdivinar(construccionPalabra(sesionVieja.getPalabraOculta(), sesionVieja.getPalabraAdivinar(), sesionNueva.getUltimaLetra()));
        } else{
            sesionNueva.setPalabraAdivinar(sesionVieja.getPalabraAdivinar());
        }

        ArrayList<String> lista = sesionVieja.getLetrasProbadas();
        String letra = quitarAcentos(sesionNueva.getUltimaLetra().toLowerCase());
        if (!lista.contains(letra) && letra.length()==1 && validarLetra(letra)) {
            lista.add(letra);
        }
        sesionNueva.setLetrasProbadas(lista);

        sesionNueva.setNumeroIntentos(sesionVieja.getNumeroIntentos());
        sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes());

    }
    private void actualizarIntentos(Datos sesionVieja, Datos sesionNueva) {

        int intentos = sesionVieja.getNumeroIntentos();
        sesionNueva.setNumeroIntentos(intentos >= 0 ? intentos + 1 : intentos);


        if (!quitarAcentos(sesionVieja.getPalabraOculta().toLowerCase())
                .contains(quitarAcentos(sesionNueva.getUltimaLetra().toLowerCase()))) {
            sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes() >= 0 ? sesionVieja.getIntentosRestantes() - 1 : intentos);
        } else{
            sesionNueva.setIntentosRestantes(sesionVieja.getIntentosRestantes());
        }
    }
    private String construccionPalabra(String palabraOculta, String palabraAdivinar, String letra) {
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
    private String unirPalabra(Datos datosFormulario){
        char[] palabraDividida = datosFormulario.getPalabraAdivinar().toCharArray();
        return new String(palabraDividida).replaceAll(" ", "");
    }

    private boolean validarLetra(String s){
        return s.matches("[a-zA-ZáéíóúüñÁÉÍÓÚÜÑ]");
    }

    private static final Pattern ACENTOS = Pattern.compile("[áéíóúüÁÉÍÓÚÜ]");
    private static final String REEMPLAZOS = "aeiouuAEIOUU";
    private String quitarAcentos(String texto) {
        if (texto == null){
            return null;
        }
        StringBuilder sb = new StringBuilder(texto);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            int index = "áéíóúüÁÉÍÓÚÜ".indexOf(c);
            if (index >= 0) sb.setCharAt(i, REEMPLAZOS.charAt(index));
        }
        return sb.toString();
    }
}
