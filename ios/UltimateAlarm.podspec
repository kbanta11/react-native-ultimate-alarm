Pod::Spec.new do |s|
  s.name           = 'UltimateAlarm'
  s.version        = '1.0.0'
  s.summary        = 'Unified alarm package for React Native'
  s.description    = 'True alarm support on Android and iOS with graceful fallbacks'
  s.author         = 'Kyle'
  s.homepage       = 'https://github.com/kylemichaelreaves/react-native-ultimate-alarm'
  s.license        = 'MIT'
  s.platform       = :ios, '13.0'
  s.source         = { git: 'https://github.com/kylemichaelreaves/react-native-ultimate-alarm.git', tag: s.version.to_s }
  s.source_files   = '*.{swift}'

  s.dependency 'ExpoModulesCore'
  s.swift_version  = '5.0'

  s.frameworks = 'UserNotifications'
  s.weak_frameworks = 'AlarmKit', 'AppIntents'
end
