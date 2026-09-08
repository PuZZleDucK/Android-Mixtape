#!/usr/bin/env ruby
# Card 4832 source-only companion to CasePlasticMaterialContractTest.
# Run without Android tooling; pixel and emulator coverage are still required.
require 'minitest/autorun'

class CasePlasticMaterialCheck < Minitest::Test
  SOURCE = File.read(File.expand_path('../app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt', __dir__))
  PREVIEW = SOURCE.split('private fun CaseComponentPreview(', 2).last.split('private fun SleeveComponentPreview(', 2).first

  def test_preview_has_no_opaque_dark_backing
    refute_includes PREVIEW, 'drawRoundRect(Color(0xFF171A20), size = size', 'Plastic outside the paper must transmit the parent background'
  end

  def test_no_heavy_independent_plastic_card_border
    refute_match(/BorderStroke\([23]\.dp, casePlastic\.edge\)/, SOURCE, 'Use a shared thin material rim')
  end

  def test_preview_does_not_bypass_runtime_tint_policy
    refute_includes PREVIEW, 'drawRoundRect(plastic.tint, size = size', 'Preview and runtime must use the same compositing policy'
  end
end
