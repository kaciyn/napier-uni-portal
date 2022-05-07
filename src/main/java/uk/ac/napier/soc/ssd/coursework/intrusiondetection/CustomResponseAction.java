//package uk.ac.napier.soc.ssd.coursework.intrusiondetection;
//
//public class CustomResponseAction implements ResponseAction {
//
//    private final ResponseAction delegee=DefaultResponseAction.getInstance();
//
//    @Override
//    public boolean handleResponse(..., AppSensorIntrusion currentIntrusion) {
//        if ("warn".equals(action)) {
//            Exception securityException = currentIntrusion.getSecurityException();
//            String localizedMessage = securityException.getLocalizedMessage();
//
//            ASUtilities asUtilities = APPSENSOR.asUtilities();
//            HttpServletRequest request = asUtilities.getCurrentRequest();
//            request.setAttribute("securityWarning", localizedMessage);
//
//            return true;
//        }
//
//        return delegee.handleResponse(action, currentIntrusion);
//    }
//
//}
//deprecated??
