module.exports = function (grunt) {
  var unsignedExtensionName = 'cssxfire_unsigned.xpi'
  var extensionPath = 'dist/' + unsignedExtensionName

  // Project configuration.
  grunt.initConfig({
                     compress: {
                       main: {
                         options: {
                           archive: extensionPath,
                           mode: 'zip'
                         },
                         files: [{expand: true, cwd: 'src/', src: ['**']}]
                       }
                     },
                     exec: {
                       jpm_sign: {
                         cmd: function () {
                           return '../node_modules/.bin/jpm sign --api-key ' + process.env.API_KEY +
                                  ' --api-secret ' + process.env.API_SECRET + ' --xpi ' + unsignedExtensionName;
                         },
                         cwd: 'dist'
                       },
                       clean: {
                         cmd: 'rm -f dist/css_x_fire*-fx.xpi',
                         stdout: false,
                         stderr: false
                       },
                       install_extension: {
                         cmd: 'cp dist/css_x_fire*-fx.xpi ../resources/com/github/cssxfire/www/cssxfire.xpi'
                       }
                     }
                   });

  grunt.loadNpmTasks('grunt-contrib-compress');
  grunt.loadNpmTasks('grunt-exec');
  grunt.registerTask('default', ['compress', 'exec:clean', 'exec:jpm_sign', 'exec:install_extension']);
};