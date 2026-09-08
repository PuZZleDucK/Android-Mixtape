#!/usr/bin/env ruby
# Rebuild review sheets from the checked-in emulator captures. Requires ImageMagick.
require 'fileutils'
require 'tmpdir'
root = File.expand_path(__dir__)
work = File.expand_path('../../../.work', root)
FileUtils.mkdir_p(work)
def run(*args)
  abort "Failed: #{args.join(' ')}" unless system(*args)
end
Dir.mktmpdir('counter-sheets-', work) do |tmp|
  crops = []
  %w[8-9 9-10 99-100].each do |transition|
    %w[00-before 015-early 02-middle 03-finished].each do |frame|
      path = File.join(tmp, "#{transition}-#{frame}.png")
      run('magick', File.join(root, 'frames', "#{transition}-#{frame}.png"),
          '-crop', '140x100+827+520', '+repage', '-scale', '300%',
          '-gravity', 'south', '-background', '#202020', '-splice', '0x32',
          '-fill', 'white', '-font', 'DejaVu-Sans', '-pointsize', '17',
          '-annotate', '+0+6', "#{transition} | #{frame}", path)
      crops << path
    end
  end
  run('magick', 'montage', *crops, '-tile', '4x3', '-geometry', '+8+8',
      '-background', '#303030', File.join(root, 'ordered-rolls.png'))
  %w[portrait landscape].each do |orientation|
    paths = Dir[File.join(root, 'skins', "#{orientation}-*-carry.png")].sort
    tiles = paths.map do |path|
      out = File.join(tmp, File.basename(path))
      label = File.basename(path).delete_prefix("#{orientation}-").delete_suffix('-carry.png')
      run('magick', path, '-resize', '420x420', '-background', '#202020',
          '-gravity', 'center', '-extent', '440x450', '-gravity', 'south',
          '-fill', 'white', '-font', 'DejaVu-Sans', '-pointsize', '16', '-annotate', '+0+5', label, out)
      out
    end
    run('magick', 'montage', *tiles, '-tile', '4x2', '-geometry', '+4+4',
        '-background', '#303030', File.join(root, "#{orientation}-skins.png"))
  end
end
