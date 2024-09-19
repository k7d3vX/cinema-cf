package com.codigofacilito.peliculas.controllers;

import com.codigofacilito.peliculas.entities.Actor;
import com.codigofacilito.peliculas.entities.Pelicula;
import com.codigofacilito.peliculas.services.IActorService;
import com.codigofacilito.peliculas.services.IArchivoService;
import com.codigofacilito.peliculas.services.IGeneroService;
import com.codigofacilito.peliculas.services.IPeliculaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class PeliculasController {

    private IPeliculaService peliculaService;
    private IGeneroService generoService;

    private IActorService actorService;

    private IArchivoService archivoService;

    public PeliculasController(IPeliculaService peliculaService, IGeneroService generoService, IActorService actorService, IArchivoService archivoService) {
        this.peliculaService = peliculaService;
        this.generoService = generoService;
        this.actorService = actorService;
        this.archivoService = archivoService;
    }

    @GetMapping("/pelicula")
    public String crear(Model model) {
        Pelicula pelicula = new Pelicula();
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("generos", generoService.findAll());
        model.addAttribute("actores", actorService.findAll());
        model.addAttribute("titulo", "Nueva Pelicula");

        return "pelicula";
    }

    @GetMapping("/pelicula/{id}")
    public String edit(@PathVariable(name = "id") Long id, Model model) {
        Pelicula pelicula = peliculaService.findById(id);
        String ids = "";
        for (Actor actor: pelicula.getProtagonistas()) {
            if (ids.isEmpty()) {
                ids = actor.getId().toString();
            } else {
                ids = ids + "," + actor.getId().toString();
            }
        }
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("ids", ids);
        model.addAttribute("actores", actorService.findAll());
        model.addAttribute("generos", generoService.findAll());
        model.addAttribute("titulo", "Editar Pelicula");

        return "pelicula";
    }

    // BindingResult tiene almacenado las validaciones del formulario.
    @PostMapping("/pelicula")
    public String guardar(@Valid Pelicula pelicula, BindingResult br, @ModelAttribute(name = "ids") String ids,
                          Model model, @RequestParam("archivo") MultipartFile imagen) {

        if (br.hasErrors()) {
            model.addAttribute("pelicula", pelicula);
            model.addAttribute("generos", generoService.findAll());
            return "/pelicula";
        }

        if (!imagen.isEmpty()) {
            String archivo = pelicula.getNombre() + getExtension(imagen.getOriginalFilename());
            pelicula.setImagen(archivo);
            try {
                archivoService.guardar(archivo, imagen.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            pelicula.setImagen("_default.jpg");
        }

        if (ids != null && !ids.isEmpty()) {
            List<Long> idsProtagonistas = Arrays.stream(ids.split(","))
                    .map(Long::parseLong).toList();
            List<Actor> protagonistas = actorService.findAllById(idsProtagonistas);
            pelicula.setProtagonistas(protagonistas);
        }
        peliculaService.save(pelicula);
        return "redirect:home";
    }

    private String getExtension(String archivo) {
        return archivo.substring(archivo.lastIndexOf("."));
    }

    @GetMapping({"/", "home", "index"})
    public String home(Model model) {
        model.addAttribute("peliculas", peliculaService.findAll());
//        model.addAttribute("msj", "Catalogo actualizado a 2023");
//        model.addAttribute("tipoMsj", "success");
        return "home";
    }

    @GetMapping({"/", "listado"})
    public String listado(Model model, @RequestParam(required = false) String msj, @RequestParam(required = false) String tipoMsj) {
        model.addAttribute("titulo", "Listado de Películas");
        model.addAttribute("peliculas", peliculaService.findAll());

        if (!"".equals(tipoMsj) && !"".equals(msj)) {
            model.addAttribute("msj", msj);
            model.addAttribute("tipoMsj", tipoMsj);
        }

        return "listado";
    }

    @GetMapping("/pelicula/{id}/delete")
    public String eliminar(@PathVariable(name = "id") Long id, Model model, RedirectAttributes redirectAttributes) {
        peliculaService.delete(id);
        redirectAttributes.addAttribute("msj", "La pelicula fue eliminada correctamente");
        redirectAttributes.addAttribute("tipoMsj", "success");
        return "redirect:/listado";
    }
}
