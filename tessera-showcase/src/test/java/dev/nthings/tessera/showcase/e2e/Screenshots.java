package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Clip;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;

/// What a visitor sees in a region of the window, reduced to colors that can be compared.
final class Screenshots {

  private Screenshots() {}

  /// The mean red, green and blue of the window region at `x`, `y` of `width` by `height`.
  static double[] meanColor(Page page, double x, double y, double width, double height) {
    var png = page.screenshot(new Page.ScreenshotOptions().setClip(new Clip(x, y, width, height)));
    BufferedImage image;
    try {
      image = ImageIO.read(new ByteArrayInputStream(png));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    var sum = new double[3];
    var pixels = 0;
    for (var row = 0; row < image.getHeight(); row++) {
      for (var column = 0; column < image.getWidth(); column++) {
        var rgb = image.getRGB(column, row);
        sum[0] += (rgb >> 16) & 0xff;
        sum[1] += (rgb >> 8) & 0xff;
        sum[2] += rgb & 0xff;
        pixels++;
      }
    }
    for (var channel = 0; channel < 3; channel++) {
      sum[channel] /= pixels;
    }
    return sum;
  }

  /// The mean color of the bottom two pixel rows of `element`, where tab bars draw the line that
  /// marks the current item.
  static double[] bottomEdge(Page page, Locator element) {
    var box = Session.box(element);
    return meanColor(page, box.x + 2, box.y + box.height - 2, box.width - 4, 2);
  }

  /// The largest per-channel difference between two colors, from 0 to 255.
  static double difference(double[] first, double[] second) {
    var largest = 0.0;
    for (var channel = 0; channel < 3; channel++) {
      largest = Math.max(largest, Math.abs(first[channel] - second[channel]));
    }
    return largest;
  }
}
