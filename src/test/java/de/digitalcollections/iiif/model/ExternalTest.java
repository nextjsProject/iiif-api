package de.digitalcollections.iiif.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.image.Size;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with real-world IIIF resources from the wild.
 */
public class ExternalTest {
  private ObjectMapper mapper;

  @Before
  public void setup() {
    this.mapper = new IiifObjectMapper();
  }

  private <T> T readFromResources(String filename, Class<T> clz) throws IOException {
    return mapper.readValue(
        Resources.getResource("external/" + filename), clz);
  }

  @Test
  public void testBiblissima() throws Exception {
    Manifest manifest = readFromResources("biblissima_ark:12148_btv1b9007608v.json", Manifest.class);
    assertThat(manifest).isNotNull();
  }

  @Test
  public void testBiblissimaReconstructed() throws Exception {
    Manifest manifest = readFromResources("biblissima_reconstructed.json", Manifest.class);
    // Two images per canvas, one page the other miniature
    assertThat(manifest.getDefaultSequence().getCanvases())
        .allMatch(c -> c.getImages().size() == 2);
  }

  @Test
  public void testThumbnailForV1Manifest() throws Exception {
    Manifest manifest = readFromResources("yale_decretum.json", Manifest.class);
    assertThat(manifest).isNotNull();
    ImageService service = manifest.getDefaultSequence().getCanvases().get(0).getImages().stream()
            .map(a -> (ImageContent) a.getResource())
            .flatMap(r -> r.getServices().stream())
            .filter(ImageService.class::isInstance)
            .map(ImageService.class::cast)
            .findFirst().orElse(null);
    ImageContent thumb;
    boolean isV1 = service.getProfiles().stream()
        .map(p -> p.getIdentifier().toString())
        .anyMatch(ImageApiProfile.V1_PROFILES::contains);
    if (isV1) {
      thumb = new ImageContent(String.format("%s/full/280,/0/native.jpg", service.getIdentifier()));
    } else {
      thumb = new ImageContent(String.format("%s/full/280,/0/default.jpg", service.getIdentifier()));
    }
    assertThat(thumb).isNotNull();
    assertThat(thumb.getIdentifier().toString()).contains("native.jpg");
  }

  @Test
  public void testWellcomeImageInfo() throws Exception {
    Service service = readFromResources("wellcome_info.json", Service.class);
    assertThat(service).isInstanceOf(ImageService.class);
    ImageService info = (ImageService) service;
    assertThat(info.getWidth()).isEqualTo(648);
    assertThat(info.getHeight()).isEqualTo(1024);
    assertThat(info.getProfiles().get(0).getIdentifier().toString()).isEqualTo(
        "http://iiif.io/api/image/2/level0.json");
    assertThat(info.getSizes()).containsExactly(
        new Size(648, 1024),
        new Size(253, 400),
        new Size(127, 200),
        new Size(63, 100));
    assertThat(info.getProfiles().get(1)).isInstanceOf(ImageApiProfile.class);
    ImageApiProfile profile = (ImageApiProfile) info.getProfiles().get(1);
    assertThat(profile.getFormats()).containsExactlyInAnyOrder(ImageApiProfile.Format.JPG);
    assertThat(profile.getQualities()).containsExactlyInAnyOrder(ImageApiProfile.Quality.COLOR);
    assertThat(profile.getFeatures()).containsExactly(ImageApiProfile.Feature.SIZE_BY_WH_LISTED);
  }

}
