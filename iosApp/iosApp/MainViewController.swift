import UIKit
import SwiftUI
import shared

class MainViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let childView = UIHostingController(rootView: ComposeView())
        addChild(childView)
        view.addSubview(childView.view)
        childView.view.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            childView.view.topAnchor.constraint(equalTo: view.topAnchor),
            childView.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            childView.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            childView.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        childView.didMove(toParent: self)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> MainViewController {
        MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: MainViewController, context: Context) {
        // Update the view controller if needed
    }
}

@main
struct iosApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
        }
    }
}
