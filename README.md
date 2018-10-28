# raptor-cage
[![Linux Build Status](https://travis-ci.org/jomof/native-scaling-benchmark-template.svg?branch=master)](https://travis-ci.org/jomof/native-scaling-benchmark-template)
[![Windows Build status](https://ci.appveyor.com/api/projects/status/y7u3dp51gnq9gm7l?svg=true)](https://ci.appveyor.com/project/jomof/native-scaling-benchmark-template)

```cmake
if( CMAKE_HOST_WIN32 )
    find_program( RAPTOR_CAGE raptor-cage.bat PATHS C:/Users/jomof/BuildServer/.package/redist )
    if (RAPTOR_CAGE)
        set_property( GLOBAL PROPERTY RULE_LAUNCH_COMPILE ${RAPTOR_CAGE} )
        set_property( GLOBAL PROPERTY RULE_LAUNCH_LINK ${RAPTOR_CAGE} )
    else()
        message( FATAL_ERROR "Could not find Raptor Cage" )
    endif()
else()
    find_program( RAPTOR_CAGE raptor-cage )
    if ( RAPTOR_CAGE )
        set_property( GLOBAL PROPERTY RULE_LAUNCH_COMPILE ${RAPTOR_CAGE} )
        set_property( GLOBAL PROPERTY RULE_LAUNCH_LINK ${RAPTOR_CAGE} )
    else()
        message( FATAL_ERROR "Could not find Raptor Cage" )
    endif()
endif()
```
