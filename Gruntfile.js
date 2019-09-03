module.exports = function (grunt) {

  const develop = process.env.NODE_ENV != 'production';

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    
    sourceDir: 'src/main/frontend',
    buildDir: 'target/frontend',
    targetDir: 'target/classes/static/',
  
    clean: {
      options: {force: true},
      'all': {
        src: ['<%= buildDir %>/*'],
      },
    },

    sass: {
      'bootstrap': {
        options: {
          style: develop ? 'expanded' : 'compressed',
          loadPath: 'node_modules/bootstrap/scss',
        },
        files: {
          '<%= buildDir %>/css/style.css': ['<%= sourceDir %>/scss/style.scss'],
        },
      },
    },

    autoprefixer: {
      'bootstrap': {
        files: {
          '<%= buildDir %>/css/style.css': '<%= buildDir %>/css/style.css',
        },
      },
    },
    
    copy: {
      options: {
        mode: '0644',
      },
      "scripts": {
        files:[{
          expand: true,
          filter: 'isFile',
          cwd: 'node_modules/bootstrap/dist',
          src: 'js/bootstrap.min.js',
          dest: '<%= targetDir %>',
        }],
      },
      "stylesheets": {
        files: [{
          expand: true,
          filter: 'isFile',
          cwd: '<%= buildDir %>',
          src: 'css/*.css',
          dest: '<%= targetDir %>',
        }],
      },
    },
  });

  // Load task modules
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-sass'); // SASS compiler for CSS
  grunt.loadNpmTasks('grunt-autoprefixer'); // autoprefixer for CSS
  
  // Tasks
  grunt.registerTask('build', ['sass', 'autoprefixer', 'copy']);
  grunt.registerTask('default', ['build']);
};
