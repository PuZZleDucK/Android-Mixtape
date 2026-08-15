#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "open3"
require "pathname"

ROOT = Pathname.new(__dir__).join("..").expand_path
MAGICK = ENV.fetch("MAGICK", "magick")

SOURCE = ROOT.join("app/icon-source/android_mixtape_logo_transparent_1024.png")
MASTER = ROOT.join("app/icon-source/ic_launcher_cassette_1024.png")

LEGACY_SIZES = {
  "mdpi" => 48,
  "hdpi" => 72,
  "xhdpi" => 96,
  "xxhdpi" => 144,
  "xxxhdpi" => 192
}.freeze

ADAPTIVE_FOREGROUND_SIZES = {
  "mdpi" => 108,
  "hdpi" => 162,
  "xhdpi" => 216,
  "xxhdpi" => 324,
  "xxxhdpi" => 432
}.freeze

MASTER_ARTWORK_RATIO = 0.85
DENSITY_ARTWORK_RATIO = 0.85

abort "Missing source artwork: #{SOURCE}" unless SOURCE.file?

def run!(*command)
  puts command.join(" ")
  stdout, stderr, status = Open3.capture3(*command)
  $stdout.write(stdout) unless stdout.empty?
  $stderr.write(stderr) unless stderr.empty?
  abort "Command failed (#{status.exitstatus}): #{command.join(" ")}" unless status.success?
end

def render_icon(source, destination, canvas_size, artwork_size)
  run!(
    MAGICK, source.to_s,
    "-trim", "+repage",
    "-filter", "Lanczos",
    "-resize", "#{artwork_size}x#{artwork_size}",
    "-background", "none", "-gravity", "center", "-extent", "#{canvas_size}x#{canvas_size}",
    "PNG32:#{destination}"
  )
end

# Make the cassette dominant but keep transparent padding for launcher masks.
render_icon(SOURCE, MASTER, 1024, (1024 * MASTER_ARTWORK_RATIO).round)

LEGACY_SIZES.keys.each { |density| FileUtils.mkdir_p(ROOT.join("app/src/main/res/mipmap-#{density}")) }

LEGACY_SIZES.each do |density, size|
  destination = ROOT.join("app/src/main/res/mipmap-#{density}/ic_launcher.png")
  render_icon(SOURCE, destination, size, (size * DENSITY_ARTWORK_RATIO).round)
end

ADAPTIVE_FOREGROUND_SIZES.each do |density, size|
  destination = ROOT.join("app/src/main/res/mipmap-#{density}/ic_launcher_foreground.png")
  render_icon(SOURCE, destination, size, (size * DENSITY_ARTWORK_RATIO).round)
end

puts "Generated transparent launcher icons from #{SOURCE.relative_path_from(ROOT)}"
