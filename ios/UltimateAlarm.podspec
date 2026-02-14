Pod::Spec.new do |s|
  s.name           = 'UltimateAlarm'
  s.version        = '1.0.0'
  s.summary        = 'Unified alarm package for React Native'
  s.description    = 'True alarm support on Android and iOS with graceful fallbacks'
  s.author         = 'Kyle'
  s.homepage       = 'https://github.com/kylemichaelreaves/react-native-ultimate-alarm'
  s.license        = 'MIT'
  s.platform       = :ios, '13.0'
  s.source         = { git: '' }
  s.source_files   = '*.{swift}'

  s.dependency 'ExpoModulesCore'
  s.swift_version  = '5.0'
end
