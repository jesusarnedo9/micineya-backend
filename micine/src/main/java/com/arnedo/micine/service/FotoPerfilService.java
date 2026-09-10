package com.arnedo.micine.service;

import com.arnedo.micine.dto.FotoPerfilResponse;
import com.arnedo.micine.entity.FotoPerfil;
import com.arnedo.micine.repository.FotoPerfilRepository;
import com.arnedo.micine.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class FotoPerfilService {
    private static final int MAX_BYTES = 64 * 1024;
    private final UsuarioRepository usuarios;
    private final FotoPerfilRepository fotos;

    public FotoPerfilService(UsuarioRepository usuarios, FotoPerfilRepository fotos) {
        this.usuarios = usuarios;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public FotoPerfilResponse obtener(String email) {
        var usuario = usuarios.findByEmail(email).orElseThrow();
        return new FotoPerfilResponse(fotos.findById(usuario.getId())
                .map(foto -> "data:image/jpeg;base64," + foto.getBase64()).orElse(null));
    }

    @Transactional
    public FotoPerfilResponse guardar(String email, String base64) {
        String normalizada = normalizar(base64);
        var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
        FotoPerfil foto = fotos.findById(usuario.getId()).orElseGet(() -> new FotoPerfil(usuario, normalizada));
        foto.setBase64(normalizada);
        fotos.save(foto);
        return new FotoPerfilResponse("data:image/jpeg;base64," + normalizada);
    }

    @Transactional
    public void quitar(String email) {
        var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
        fotos.deleteById(usuario.getId());
    }

    private String normalizar(String base64) {
        if (base64 == null || base64.length() > 87384) {
            throw new IllegalArgumentException("La foto debe pesar como máximo 64 KB");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("La foto es demasiado grande");
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Elegí una foto JPEG válida");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (!"JPEG".equalsIgnoreCase(reader.getFormatName())
                            || width < 1 || height < 1 || width > 512 || height > 512) {
                        throw new IllegalArgumentException("La foto debe ser JPEG y medir hasta 512 × 512 píxeles");
                    }
                    BufferedImage original = reader.read(0);
                    int side = Math.min(width, height);
                    var cropped = original.getSubimage((width - side) / 2, (height - side) / 2, side, side);
                    var result = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                    var graphics = result.createGraphics();
                    try {
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(cropped, 0, 0, 256, 256, null);
                    } finally {
                        graphics.dispose();
                    }
                    var output = new ByteArrayOutputStream();
                    ImageIO.write(result, "jpeg", output);
                    if (output.size() > MAX_BYTES) throw new IllegalArgumentException("Probá con otra foto más liviana");
                    // Recodificar elimina metadatos, como ubicación, y descarta el archivo original.
                    return Base64.getEncoder().encodeToString(output.toByteArray());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No pudimos leer la foto. Elegí otra imagen");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La foto no es válida o supera el tamaño permitido");
        }
    }
}
