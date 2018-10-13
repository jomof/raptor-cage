# raptor-cage

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
