#!/usr/bin/env ruby
# Read the raw emulator captures; produce labeled comparisons and thresholded ink measurements.
require 'open3'
require 'csv'
require 'fileutils'

root = File.expand_path(ARGV.fetch(0, 'raw'), __dir__)
output = File.expand_path(ARGV.fetch(1, '.'), __dir__)
fonts = %w[Kalam PatrickHand Caveat NanumPenScript IndieFlower GloriaHallelujah ArchitectsDaughter ShadowsIntoLight]
# Framebuffer rectangles inspected in the raw captures: x, y, width, height.
rects = {
  1 => { 'list' => [58, 263, 964, 234], 'playing-0' => [32, 821, 827, 201] },
  2 => { 'list' => [58, 263, 828, 201], 'playing-0' => [32, 639, 572, 139] }
}

def command(*args, input: '')
  data, error, status = Open3.capture3(*args, stdin_data: input)
  abort "#{args.inspect}: #{error}" unless status.success?
  data
end

def crop_pixels(path, rect)
  x, y, w, h = rect
  command('magick', path, '-crop', "#{w}x#{h}+#{x}+#{y}", '+repage', '-depth', '8', 'rgb:-').bytes
end

FileUtils.mkdir_p(output)
rows = []
rects.each do |orientation, contexts|
  images = []
  fonts.each do |font|
    contexts.each do |context, rect|
      %w[before after].each do |phase|
        path = File.join(root, "#{phase}-#{orientation}", "#{font}-#{context}.png")
        x, y, w, h = rect
        # Include badge in comparisons, but exclude it and all reserved margins from measurements.
        viewport = [(56.0 * w / 280).round, (12.0 * h / 68).round,
                    w - (18.0 * w / 280).round, h - (12.0 * h / 68).round]
        pixels = crop_pixels(path, rect)
        left, top, right, bottom = w, h, 0, 0
        viewport[1].upto(viewport[3] - 1) do |py|
          viewport[0].upto(viewport[2] - 1) do |px|
            rgb = pixels[(py * w + px) * 3, 3]
            next unless rgb.max < 130
            left = [left, px].min; right = [right, px + 1].max
            top = [top, py].min; bottom = [bottom, py + 1].max
          end
        end
        dx = (left + right - viewport[0] - viewport[2]) / 2.0
        dy = (top + bottom - viewport[1] - viewport[3]) / 2.0
        rows << [orientation, context, font, phase, viewport.join(':'), [left, top, right, bottom].join(':'), dx, dy]
        # Parenthesized ImageMagick groups are argv, not shell fragments. No scratch images.
        images += ['(', path, '-gravity', 'northwest', '-crop', "#{w}x#{h}+#{x}+#{y}", '+repage', '-resize', '420x',
                   '-background', 'white', '-fill', '#252930', '-font', 'DejaVu-Sans', '-gravity', 'north',
                   '-splice', '0x32', '-pointsize', '16', '-annotate', '+0+6', "#{font} #{context} #{phase}", ')']
      end
    end
  end
  frames = command('magick', *images, 'miff:-')
  command('magick', 'montage', 'miff:-', '-tile', '4x', '-geometry', '+4+4', '-background', '#dedede',
          File.join(output, "comparison-#{orientation}.png"), input: frames)
end
CSV.open(File.join(output, 'screen-ink-bounds.csv'), 'w') do |csv|
  csv << %w[orientation context font phase title_viewport_ltrb ink_ltrb horizontal_error_px vertical_error_px]
  rows.each { |row| csv << row }
end

# Pixel-equality checks on overflow, badges and untouched track-list writing.
CSV.open(File.join(output, 'unchanged-regions.csv'), 'w') do |csv|
  csv << %w[orientation font region equal_rgb_bytes]
  rects.each do |orientation, contexts|
    fonts.each do |font|
      contexts.each do |context, (x, y, w, h)|
        badge = [x, y, (w * 0.20).to_i, h]
        paths = %w[before after].map { |phase| File.join(root, "#{phase}-#{orientation}", "#{font}-#{context}.png") }
        csv << [orientation, font, "#{context}-badge", crop_pixels(paths[0], badge) == crop_pixels(paths[1], badge)]
      end
      x, y, w, h = contexts.fetch('playing-0')
      paths = %w[before after].map { |phase| File.join(root, "#{phase}-#{orientation}", "#{font}-playing-1.png") }
      csv << [orientation, font, 'overflow-spine', crop_pixels(paths[0], [x,y,w,h]) == crop_pixels(paths[1], [x,y,w,h])]
      track_rect = orientation == 1 ? [32, 1043, 1016, 730] : [825, 84, 937, 975]
      csv << [orientation, font, 'track-list', crop_pixels(paths[0], track_rect) == crop_pixels(paths[1], track_rect)]
    end
  end
end
preview_images = []
overflow_images = []
fonts.each_with_index do |font, index|
  [1, 2].each do |orientation|
    %w[before after].each do |phase|
      source_font = orientation == 1 && index == 6 ? 'GloriaHallelujah' : font
      x, y, w, h = if orientation == 1
        [102, index == 6 ? 1148 : index == 7 ? 992 : 340, 876, 214]
      else
        [index.even? ? 102 : 951, 340, 742, 180]
      end
      path = File.join(root, "#{phase}-#{orientation}", "#{source_font}-preview.png")
      preview_images += ['(', path, '-gravity', 'northwest', '-crop', "#{w}x#{h}+#{x}+#{y}", '+repage', '-resize', '420x',
                        '-background', 'white', '-fill', '#252930', '-font', 'DejaVu-Sans', '-gravity', 'north',
                        '-splice', '0x32', '-pointsize', '16', '-annotate', '+0+6', "#{font} #{orientation} #{phase}", ')']
      x, y, w, h = rects.fetch(orientation).fetch('playing-0')
      path = File.join(root, "#{phase}-#{orientation}", "#{font}-playing-1.png")
      overflow_images += ['(', path, '-gravity', 'northwest', '-crop', "#{w}x#{h}+#{x}+#{y}", '+repage', '-resize', '420x',
                         '-background', 'white', '-fill', '#252930', '-font', 'DejaVu-Sans', '-gravity', 'north',
                         '-splice', '0x32', '-pointsize', '16', '-annotate', '+0+6', "#{font} #{orientation} #{phase}", ')']
    end
  end
end
{ 'previews' => preview_images, 'overflow' => overflow_images }.each do |name, images|
  frames = command('magick', *images, 'miff:-')
  command('magick', 'montage', 'miff:-', '-tile', '4x', '-geometry', '+4+4', '-background', '#dedede',
          File.join(output, "comparison-#{name}.png"), input: frames)
end
puts "Wrote comparison sheets, ink bounds and unchanged-region checks to #{output}"
