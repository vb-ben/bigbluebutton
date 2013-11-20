/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.web.controllers

import grails.converters.*
import org.bigbluebutton.web.services.PresentationService
import org.bigbluebutton.presentation.UploadedPresentation

class PresentationController {
  PresentationService presentationService
  
  def index = {
    println 'in PresentationController index'
    render(view:'upload-file') 
  }
  
  def list = {						      				
    def f = confInfo()
    println "conference info ${f.conference} ${f.room}"
    def presentationsList = presentationService.listPresentations(f.conference, f.room)

    if (presentationsList) {
      withFormat {				
        xml {
          render(contentType:"text/xml") {
            conference(id:f.conference, room:f.room) {
              presentations {
                for (s in presentationsList) {
                  presentation(name:s)
                }
              }
            }
          }
        }
      }
    } else {
      render(view:'upload-file')
    }
  }

  def delete = {		
    def filename = params.presentation_name
    def f = confInfo()
    presentationService.deletePresentation(f.conference, f.room, filename)
    flash.message = "file ${filename} removed" 
    redirect( action:list )
  }

  def upload = {		
    println 'PresentationController:upload'
    def file = request.getFile('fileUpload')
		if(file && !file.empty) {
			flash.message = 'Your file has been uploaded'
			// Replace any character other than a (A-Z, a-z, 0-9, _ or .) with a - (dash).
			def notValidCharsRegExp = /[^0-9a-zA-Z_\.]/
			log.debug "Uploaded presentation name : $params.presentation_name"
			def presentationName = params.presentation_name.replaceAll(notValidCharsRegExp, '-')
			log.debug "Uploaded presentation name : $presentationName"
			File uploadDir = presentationService.uploadedPresentationDirectory(params.conference, params.room, presentationName)
	
			def newFilename = file.getOriginalFilename().replaceAll(notValidCharsRegExp, '-')
			def pres = new File( uploadDir.absolutePath + File.separatorChar + newFilename )
			file.transferTo(pres)	
	      
			UploadedPresentation uploadedPres = new UploadedPresentation(params.conference, params.room, presentationName);
			uploadedPres.setUploadedFile(pres);
			presentationService.processUploadedPresentation(uploadedPres)							             			     	
		} else {
			flash.message = 'file cannot be empty'
		}
    //redirect( action:list)
    return [];
  }

  def testConversion = {
    presentationService.testConversionProcess();
  }

  //handle external presentation server 
  def delegate = {		
    println '\nPresentationController:delegate'
    
    def presentation_name = request.getParameter('presentation_name')
    def conference = request.getParameter('conference')
    def room = request.getParameter('room')
    def returnCode = request.getParameter('returnCode')
    def totalSlides = request.getParameter('totalSlides')
    def slidesCompleted = request.getParameter('slidesCompleted')
    
        presentationService.processDelegatedPresentation(conference, room, presentation_name, returnCode, totalSlides, slidesCompleted)
    redirect( action:list)
  }
  
  def showSlide = {
    def presentationName = params.presentation_name
    def conf = params.conference
    def rm = params.room
    def slide = params.id
    
    InputStream is = null;
    try {
//			def f = confInfo()
      def pres = presentationService.showSlide(conf, rm, presentationName, slide)
      if (pres.exists()) {
        def bytes = pres.readBytes()
        response.addHeader("Cache-Control", "no-cache")
        response.contentType = 'application/x-shockwave-flash'
        response.outputStream << bytes;
      }	
    } catch (IOException e) {
      System.out.println("Error reading file.\n" + e.getMessage());
    }
    
    return null;
  }
  
  def showThumbnail = {
    
    def presentationName = params.presentation_name
    def conf = params.conference
    def rm = params.room
    def thumb = params.id
    println "Controller: Show thumbnails request for $presentationName $thumb"
    
    InputStream is = null;
    try {
      def pres = presentationService.showThumbnail(conf, rm, presentationName, thumb)
      if (pres.exists()) {
        println "Controller: Sending thumbnails reply for $presentationName $thumb"
        
        def bytes = pres.readBytes()
        response.addHeader("Cache-Control", "no-cache")
        response.contentType = 'image'
        response.outputStream << bytes;
      } else {
        println "$pres does not exist."
      }
    } catch (IOException e) {
      println("Error reading file.\n" + e.getMessage());
    }
    
    return null;
  }
  
  def showTextfile = {
	  def presentationName = params.presentation_name
	  def conf = params.conference
	  def rm = params.room
	  def textfile = params.id
	  println "Controller: Show thumbnails request for $presentationName $textfile"
	  
	  InputStream is = null;
	  try {
		def pres = presentationService.showTextfile(conf, rm, presentationName, textfile)
		if (pres.exists()) {
		  println "Controller: Sending textfiles reply for $presentationName $textfile"
		  
		  def bytes = pres.readBytes()
		  response.addHeader("Cache-Control", "no-cache")
		  response.contentType = 'plain/text'
		  response.outputStream << bytes;
		} else {
		  println "$pres does not exist."
		}
	  } catch (IOException e) {
		println("Error reading file.\n" + e.getMessage());
	  }
	  
	  return null;
  }
  
  def show = {
    //def filename = params.id.replace('###', '.')
    def filename = params.presentation_name
    InputStream is = null;
    System.out.println("showing ${filename}")
    try {
      def f = confInfo()
      def pres = presentationService.showPresentation(f.conference, f.room, filename)
      if (pres.exists()) {
        System.out.println("Found ${filename}")
        def bytes = pres.readBytes()

        response.contentType = 'application/x-shockwave-flash'
        response.outputStream << bytes;
      }	
    } catch (IOException e) {
      System.out.println("Error reading file.\n" + e.getMessage());
    }
    
    return null;
  }
  
  def thumbnail = {
    def filename = params.id.replace('###', '.')
    System.out.println("showing ${filename} ${params.thumb}")
    def presDir = confDir() + File.separatorChar + filename
    try {
      def pres = presentationService.showThumbnail(presDir, params.thumb)
      if (pres.exists()) {
        def bytes = pres.readBytes()

        response.contentType = 'image'
        response.outputStream << bytes;
      }	
    } catch (IOException e) {
      System.out.println("Error reading file.\n" + e.getMessage());
    }
    
    return null;
  }

  def numberOfSlides = {
    def presentationName = params.presentation_name
    def conf = params.conference
    def rm = params.room

    System.out.println("test showing ${conf} ${rm}")
    
    def numThumbs = presentationService.numberOfThumbnails(conf, rm, presentationName)
      response.addHeader("Cache-Control", "no-cache")

      def roomAbsolutePath = presentationService.roomDirectory(rm, rm).absolutePath
      def presentationPath = roomAbsolutePath + '/' + presentationName
      def docPath = presentationPath + '/documentData.txt'
      
      def docFiile = new File(docPath)
      def s3Prefix = "http://lessonslidesmyngle.s3.amazonaws.com/$rm/Preloaded/Preloaded/$presentationName"

    
      if (docFiile.exists()) {
        withFormat {            
          xml {
            render(contentType:"text/xml") {
              conference(id:conf, room:rm) {
                presentation(name:presentationName) {
                  slides(count:numThumbs) {
                    def firstSlide = new File(presentationPath + '/0.swf')
                    def firstSlideExist = firstSlide.exists()

                    for (def i = 1; i <= numThumbs; i++) {
                      if (firstSlideExist) {
                        slide(number:"${i}", name:"slide/${i}", thumb:"slide/1", textfile:"slide/1")
                      } else {
                        slide(number:"${i}", name:"$s3Prefix/${i-1}.swf", thumb:"$s3Prefix/${i-1}.swf", textfile:"$s3Prefix/${i-1}.swf")
                      }

                    }
                  }
                }
              }
            }
          }
        }       
      } else {

        withFormat {						
          xml {
            render(contentType:"text/xml") {
              conference(id:conf, room:rm) {
                presentation(name:presentationName) {
                  slides(count:numThumbs) {
                    for (def i = 1; i <= numThumbs; i++) {
                      slide(number:"${i}", name:"slide/${i}", thumb:"thumbnail/${i}", textfile:"textfile/${i}")
                    }
                  }
                }
              }
            }
          }
        }		
    }
  }
    
  def numberOfThumbnails = {
    def filename = params.presentation_name
    def f = confInfo()
    def numThumbs = presentationService.numberOfThumbnails(f.conference, f.room, filename)
      withFormat {				
        xml {
          render(contentType:"text/xml") {
            conference(id:f.conference, room:f.room) {
              presentation(name:filename) {
                thumbnails(count:numThumbs) {
                  for (def i=0;i<numThumbs;i++) {
                      thumb(name:"thumbnails/${i}")
                    }
                }
              }
            }
          }
        }
      }		
  }
  
  def numberOfTextfiles = {
	  def filename = params.presentation_name
	  def f = confInfo()
	  def numFiles = presentationService.numberOfTextfiles(f.conference, f.room, filename)
		withFormat {
		  xml {
			render(contentType:"text/xml") {
			  conference(id:f.conference, room:f.room) {
				presentation(name:filename) {
				  textfiles(count:numFiles) {
					for (def i=0;i<numFiles;i++) {
						textfile(name:"textfiles/${i}")
					  }
				  }
				}
			  }
			}
		  }
		}
	}
  
  def confInfo = {
//    	Subject currentUser = SecurityUtils.getSubject() 
//		Session session = currentUser.getSession()

      def fname = session["fullname"]
      def rl = session["role"]
      def conf = session["conference"]
      def rm = session["room"]
      println "Conference info: ${conf} ${rm}"
      return [conference:conf, room:rm]
  }
}