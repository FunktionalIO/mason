package mason.webapp

import scala.util.control.NoStackTrace

enum Error extends RuntimeException, NoStackTrace:
    case MalformedBackendUri(input: String)